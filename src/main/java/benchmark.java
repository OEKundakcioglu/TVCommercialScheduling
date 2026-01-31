import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.ProblemParameters;
import data.problemBuilders.JsonParser;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Comprehensive benchmark for evaluating GRASP improvements:
 * - InterSwapSearch bug fix (shuffled indices)
 * - New moves: remove and chainSwap (3-way rotation)
 * - MoveStatistics for per-move performance tracking
 * - Various configuration comparisons
 * <p>
 * Configurations are auto-generated from parameter lists (factorial design).
 */
public class benchmark {

    // ==================== PARAMETER SPACE ====================

    // Boolean parameters
    static final List<Boolean> ADAPTIVE_MOVES_OPTIONS = List.of(false, true);

    // Alpha generator options
    static final List<String> ALPHA_TYPES = List.of("reactive", "fixed");

    // Alpha generator parameters
    static final List<Double> FIXED_ALPHA_VALUES = List.of(0.5);
    static final double UNIFORM_ALPHA_MIN = 0.1;
    static final double UNIFORM_ALPHA_MAX = 0.9;

    // Other tunable parameters (as lists for experimentation)
    static final List<Double> SKIP_PROBABILITIES = List.of(0.0, 0.1, 0.5);

    // ==================== MOVE SET CONFIGURATION ====================

    // Base moves available for subset generation
    static final List<String> BASE_MOVES = List.of(
            "insert", "outOfPool", "interSwap", "shift", "transfer", "intraSwap", "chainSwap"
    );

    // Number of random move subsets to sample (0 = use FIXED_MOVE_SET only)
    static final int NUM_MOVE_SUBSETS = 100;

    // Fixed move set to use when NUM_MOVE_SUBSETS = 0 (null = use full BASE_MOVES)
    static final List<String> FIXED_MOVE_SET = null;
    // Example: List.of("insert", "outOfPool", "interSwap", "shift")

    // Minimum number of moves required in each subset
    static final int MIN_MOVES_IN_SUBSET = 4;

    // Seed for reproducible move subset sampling
    static final long MOVE_SUBSET_SEED = 42;

    // Pre-generated move set configurations
    static final List<List<String>> MOVE_SET_CONFIGS = generateMoveSetConfigurations();

    // ==================== CONFIGURATION DEFINITIONS ====================
    // Auto-generated configurations from parameter space
    static final List<BenchmarkConfig> CONFIGS = generateConfigurations();
    // Priority instances (larger, harder - more gap potential)
    static final String[] PRIORITY_INSTANCES = {
            "instances/density=HIGH_nInventory=20_nHours=5_seed=1.json",
//            "instances/density=HIGH_nInventory=15_nHours=4_seed=1.json",
//            "instances/density=MEDIUM_nInventory=20_nHours=5_seed=1.json",
//            "instances/density=MEDIUM_nInventory=15_nHours=4_seed=1.json",
//            "instances/density=LOW_nInventory=20_nHours=5_seed=1.json",
//            "instances/density=LOW_nInventory=15_nHours=4_seed=1.json"
    };
    // Secondary instances (smaller, for validation)
    static final String[] SECONDARY_INSTANCES = {
//            "instances/density=HIGH_nInventory=10_nHours=3_seed=1.json",
//            "instances/density=MEDIUM_nInventory=10_nHours=3_seed=1.json",
//            "instances/density=LOW_nInventory=10_nHours=3_seed=1.json"
    };
    static final int THREAD_COUNT = 8;                    // Number of parallel threads
    static final int TIME_LIMIT_PER_RUN_SECONDS = 50;     // Time limit per run in seconds (0 = auto-compute from duration)
    static final int BENCHMARK_DURATION_MINUTES = 60;      // Total wall-clock duration (used when TIME_LIMIT_PER_RUN_SECONDS = 0)

    // ==================== TEST INSTANCES ====================
    static final int[] SEEDS = {0, 1, 2, 3, 4, 5}; // full seed set
    static final int OVERNIGHT_SEEDS = 1;       // reduced for overnight (~8 hours)

    // ==================== PARALLELIZATION PARAMETERS ====================
    static final int QUICK_TEST_SEEDS = 1;      // for quick validation
    static final int QUICK_TEST_TIME_LIMIT = 10;
    private static final Object csvLock = new Object();

    // If TIME_LIMIT_PER_RUN_SECONDS > 0: uses fixed time limit per run
    // If TIME_LIMIT_PER_RUN_SECONDS = 0: auto-computes as (BENCHMARK_DURATION_MINUTES * THREAD_COUNT * 60) / numRuns

    // ==================== PARAMETERS ====================
    private static final Object moveStatsCsvLock = new Object();
    private static boolean csvHeaderWritten = false;
    private static boolean moveStatsHeaderWritten = false;

    /**
     * Generates all possible subsets of the given list (power set).
     */
    private static <T> List<List<T>> generatePowerSet(List<T> items) {
        List<List<T>> powerSet = new ArrayList<>();
        int n = items.size();

        // Generate all 2^n subsets using bit manipulation
        for (int mask = 0; mask < (1 << n); mask++) {
            List<T> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(items.get(i));
                }
            }
            powerSet.add(subset);
        }
        return powerSet;
    }

    // ==================== REAL-TIME CSV WRITING ====================

    /**
     * Generates move set configurations: either samples N random subsets or uses fixed set.
     */
    private static List<List<String>> generateMoveSetConfigurations() {
        if (NUM_MOVE_SUBSETS <= 0) {
            // Use only the fixed move set (or full BASE_MOVES if not specified)
            List<String> moveSet = FIXED_MOVE_SET != null ? FIXED_MOVE_SET : BASE_MOVES;
            return List.of(new ArrayList<>(moveSet));
        }

        // Generate all subsets
        List<List<String>> allSubsets = generatePowerSet(BASE_MOVES);

        // Filter by minimum move count
        List<List<String>> validSubsets = allSubsets.stream()
                .filter(subset -> subset.size() >= MIN_MOVES_IN_SUBSET)
                .collect(Collectors.toList());

        // Shuffle with fixed seed for reproducibility
        Random random = new Random(MOVE_SUBSET_SEED);
        Collections.shuffle(validSubsets, random);

        // Take up to NUM_MOVE_SUBSETS
        int count = Math.min(NUM_MOVE_SUBSETS, validSubsets.size());
        return validSubsets.subList(0, count);
    }

    /**
     * Generates all configurations as Cartesian product of parameter lists.
     */
    private static List<BenchmarkConfig> generateConfigurations() {
        List<BenchmarkConfig> configs = new ArrayList<>();

        for (List<String> moveSet : MOVE_SET_CONFIGS) {
            for (boolean adaptive : ADAPTIVE_MOVES_OPTIONS) {
                for (String alphaType : ALPHA_TYPES) {
                    for (double fixedAlpha : FIXED_ALPHA_VALUES) {
                        for (double skip : SKIP_PROBABILITIES) {
                            // Skip fixedAlpha variations when not using fixed alpha
                            if (!alphaType.equals("fixed") &&
                                    FIXED_ALPHA_VALUES.indexOf(fixedAlpha) > 0) {
                                continue;
                            }

                            String name = buildConfigName(moveSet, adaptive, alphaType,
                                    fixedAlpha, skip);

                            configs.add(new BenchmarkConfig(
                                    name, moveSet, adaptive, alphaType,
                                    fixedAlpha, skip
                            ));
                        }
                    }
                }
            }
        }

        return configs;
    }

    /**
     * Builds a descriptive name for a configuration based on its parameters.
     */
    private static String buildConfigName(List<String> moveSet, boolean adaptive,
                                          String alphaType, double fixedAlpha, double skip) {
        StringBuilder sb = new StringBuilder();

        // Add move set identifier if using subsets
        if (MOVE_SET_CONFIGS.size() > 1) {
            sb.append("m").append(moveSet.size()).append("_");
            // Add abbreviated move names
            sb.append(moveSet.stream()
                    .map(m -> m.substring(0, Math.min(2, m.length())))
                    .collect(Collectors.joining("")));
            sb.append("_");
        }

        sb.append(adaptive ? "adaptive" : "static");
        sb.append("_").append(alphaType);
        if (alphaType.equals("fixed")) {
            sb.append(String.format("%.1f", fixedAlpha));
        }

        // Only add non-default values to name when multiple options exist
        if (SKIP_PROBABILITIES.size() > 1) {
            sb.append("_skip").append(String.format("%.1f", skip));
        }

        return sb.toString();
    }

    private static void appendResultToCSV(String path, String[] row) {
        synchronized (csvLock) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(path, true))) {
                if (!csvHeaderWritten) {
                    writer.println("instance,config,seed,revenue,reference_revenue,gap_pct,time_to_best_s,iterations,iter_per_sec");
                    csvHeaderWritten = true;
                }
                writer.println(String.join(",", row));
            } catch (Exception e) {
                System.err.println("Error appending to CSV: " + e.getMessage());
            }
        }
    }

    private static void appendMoveStatsToCSV(String path, List<String[]> rows) {
        synchronized (moveStatsCsvLock) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(path, true))) {
                if (!moveStatsHeaderWritten) {
                    writer.println("instance,config,seed,move_name,attempts,successes,success_rate_pct,avg_gain,avg_time_ms");
                    moveStatsHeaderWritten = true;
                }
                for (String[] row : rows) {
                    writer.println(String.join(",", row));
                }
            } catch (Exception e) {
                System.err.println("Error appending to move stats CSV: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        boolean quickTest = false;
        boolean includeSecondary = true;
        Set<String> selectedGroups = null;

        // Parse command-line arguments
        for (String arg : args) {
            if (arg.equals("--quick") || arg.equals("-q")) {
                quickTest = true;
            } else if (arg.equals("--priority-only")) {
                includeSecondary = false;
            } else if (arg.startsWith("--groups=")) {
                selectedGroups = new HashSet<>(Arrays.asList(arg.substring(9).split(",")));
            }
        }

        if (quickTest) {
            runQuickTest();
        } else {
            runFullBenchmark(includeSecondary, selectedGroups);
        }
    }

    // ==================== RESULT HOLDERS ====================

    private static void runQuickTest() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("QUICK TEST MODE");
        System.out.println("1 instance, 2 configs, 1 seed, " + QUICK_TEST_TIME_LIMIT + "s time limit");
        System.out.println("=".repeat(70));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputDir = "benchmark_results/" + timestamp + "_quicktest";
        Files.createDirectories(Paths.get(outputDir));

        // Use one priority instance
        String testInstance = PRIORITY_INSTANCES[0];
        if (!Files.exists(Paths.get(testInstance))) {
            System.out.println("Test instance not found: " + testInstance);
            return;
        }

        // Load reference solutions
        Map<String, Integer> referenceSolutions = loadReferenceSolutions();

        // Select two configs for quick comparison (first and last from generated list)
        List<BenchmarkConfig> quickConfigs = List.of(
                CONFIGS.stream().filter(c -> c.name.equals("static_reactive_noPert")).findFirst().orElse(CONFIGS.get(0)),
                CONFIGS.stream().filter(c -> c.name.equals("adaptive_reactive_pert")).findFirst().orElse(CONFIGS.get(CONFIGS.size() - 1))
        );

        List<BenchmarkResult> results = new ArrayList<>();
        List<String[]> resultRows = new ArrayList<>();
        List<String[]> moveStatRows = new ArrayList<>();

        String instanceName = Paths.get(testInstance).getFileName().toString().replace(".json", "");
        Integer referenceRevenue = referenceSolutions.get(instanceName);

        System.out.println("\nInstance: " + instanceName);
        if (referenceRevenue != null) {
            System.out.println("Reference solution: " + referenceRevenue);
        }

        ProblemParameters problem = new JsonParser().readData(testInstance);

        for (BenchmarkConfig config : quickConfigs) {
            System.out.println("\n--- Config: " + config.name + " ---");

            BenchmarkResult result = runSingleBenchmark(
                    problem, testInstance, 0, config, QUICK_TEST_TIME_LIMIT
            );

            results.add(result);
            resultRows.add(formatResultRow(instanceName, config.name, 0, result, referenceRevenue));

            if (result.moveStats != null) {
                collectMoveStatRows(instanceName, config.name, 0, result.moveStats, moveStatRows);
            }

            printResult(config.name, result, referenceRevenue);
        }

        // Write results
        writeResultsCSV(outputDir + "/results.csv", resultRows);
        writeMoveStatisticsCSV(outputDir + "/move_statistics.csv", moveStatRows);
        writeConfigSummary(outputDir + "/config_summary.txt", quickConfigs, QUICK_TEST_TIME_LIMIT);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("Quick test complete! Results saved to: " + outputDir);
        System.out.println("=".repeat(70));
    }

    // ==================== MAIN ENTRY POINT ====================

    private static void runFullBenchmark(boolean includeSecondary, Set<String> selectedGroups) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputDir = "benchmark_results/" + timestamp + "_overnight";
        Files.createDirectories(Paths.get(outputDir));

        System.out.println("=".repeat(70));
        System.out.println("COMPREHENSIVE PARALLEL BENCHMARK");
        System.out.println("Output directory: " + outputDir);
        System.out.println("=".repeat(70));

        // Collect instances
        List<String> instances = new ArrayList<>(Arrays.asList(PRIORITY_INSTANCES));
        if (includeSecondary) {
            instances.addAll(Arrays.asList(SECONDARY_INSTANCES));
        }

        // Filter to existing instances
        instances.removeIf(path -> !Files.exists(Paths.get(path)));

        // Collect configs (filter by group if specified)
        List<BenchmarkConfig> configs;
        if (selectedGroups == null) {
            configs = new ArrayList<>(CONFIGS);
        } else {
            configs = new ArrayList<>();
            for (BenchmarkConfig config : CONFIGS) {
                // Group filtering is no longer directly supported with auto-generated configs
                // For backward compatibility, include all configs when groups are specified
                configs.add(config);
            }
        }

        // Load reference solutions
        Map<String, Integer> referenceSolutions = loadReferenceSolutions();

        // Calculate total runs and time limit
        int numSeeds = OVERNIGHT_SEEDS;
        int totalRuns = instances.size() * configs.size() * numSeeds;

        int timeLimit;
        String timeLimitMode;
        if (TIME_LIMIT_PER_RUN_SECONDS > 0) {
            timeLimit = TIME_LIMIT_PER_RUN_SECONDS;
            timeLimitMode = "fixed";
        } else {
            int computeBudgetSeconds = BENCHMARK_DURATION_MINUTES * THREAD_COUNT * 60;
            timeLimit = Math.max(10, computeBudgetSeconds / totalRuns);
            timeLimitMode = "auto (" + BENCHMARK_DURATION_MINUTES + " min × " + THREAD_COUNT + " threads / " + totalRuns + " runs)";
        }

        System.out.println("\nBenchmark Configuration:");
        System.out.println("  - Instances: " + instances.size());
        System.out.println("  - Configurations: " + configs.size());
        System.out.println("  - Seeds per run: " + numSeeds);
        System.out.println("  - Total runs: " + totalRuns);
        System.out.println("  - Thread count: " + THREAD_COUNT);
        System.out.println("  - Time limit per run: " + timeLimit + "s (" + timeLimitMode + ")");
        System.out.println("  - Reference solutions loaded: " + referenceSolutions.size());
        System.out.println();

        // Print config groups
        printConfigGroups(configs);

        // Thread-safe result collections
        ConcurrentLinkedQueue<String[]> resultRows = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String[]> moveStatRows = new ConcurrentLinkedQueue<>();
        AtomicInteger completedRuns = new AtomicInteger(0);

        // Pre-load all problems (thread-safe read)
        Map<String, ProblemParameters> problemCache = new ConcurrentHashMap<>();
        for (String instancePath : instances) {
            try {
                problemCache.put(instancePath, new JsonParser().readData(instancePath));
            } catch (Exception e) {
                System.err.println("Failed to load instance: " + instancePath + " - " + e.getMessage());
            }
        }

        // Initialize CSV paths and reset flags for new benchmark run
        String resultsPath = outputDir + "/results.csv";
        String moveStatsPath = outputDir + "/move_statistics.csv";
        csvHeaderWritten = false;
        moveStatsHeaderWritten = false;

        long benchmarkStart = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Void>> futures = new ArrayList<>();

        // Submit all benchmark tasks
        for (String instancePath : instances) {
            String instanceName = Paths.get(instancePath).getFileName().toString().replace(".json", "");
            Integer referenceRevenue = referenceSolutions.get(instanceName);
            ProblemParameters problem = problemCache.get(instancePath);

            if (problem == null) continue;

            for (BenchmarkConfig config : configs) {
                for (int seedIdx = 0; seedIdx < numSeeds; seedIdx++) {
                    int seed = SEEDS[seedIdx];
                    final int finalTimeLimit = timeLimit;

                    futures.add(executor.submit(() -> {
                        try {
                            BenchmarkResult result = runSingleBenchmark(
                                    problem, instancePath, seed, config, finalTimeLimit
                            );

                            String[] row = formatResultRow(instanceName, config.name, seed, result, referenceRevenue);
                            resultRows.add(row);

                            // Write to CSV immediately
                            appendResultToCSV(resultsPath, row);

                            if (result.moveStats != null) {
                                List<String[]> localMoveStats = new ArrayList<>();
                                collectMoveStatRows(instanceName, config.name, seed, result.moveStats, localMoveStats);
                                moveStatRows.addAll(localMoveStats);

                                // Write move stats to CSV immediately
                                appendMoveStatsToCSV(moveStatsPath, localMoveStats);
                            }

                            int completed = completedRuns.incrementAndGet();
                            printProgressParallel(completed, totalRuns, benchmarkStart, instanceName, config, seed, result, referenceRevenue);

                        } catch (Exception e) {
                            System.err.println("Error in run [" + instanceName + "/" + config.name + "/seed=" + seed + "]: " + e.getMessage());
                        }
                        return null;
                    }));
                }
            }
        }

        // Wait for all tasks to complete
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.err.println("Task execution error: " + e.getMessage());
            }
        }

        executor.shutdown();

        // Convert concurrent queues to lists for writing
        List<String[]> finalResultRows = new ArrayList<>(resultRows);
        List<String[]> finalMoveStatRows = new ArrayList<>(moveStatRows);

        // Final write
        writeResultsCSV(outputDir + "/results.csv", finalResultRows);
        writeMoveStatisticsCSV(outputDir + "/move_statistics.csv", finalMoveStatRows);
        writeConfigSummary(outputDir + "/config_summary.txt", configs, timeLimit);

        // Print summary
        long totalTime = (System.currentTimeMillis() - benchmarkStart) / 1000;
        System.out.println("\n" + "=".repeat(70));
        System.out.println("BENCHMARK COMPLETE");
        System.out.println("  - Total runs: " + completedRuns.get());
        System.out.println("  - Wall-clock time: " + totalTime + "s (" + (totalTime / 60) + " minutes)");
        System.out.println("  - Results saved to: " + outputDir);
        System.out.println("=".repeat(70));

        // Print summary statistics
        printSummaryStatistics(finalResultRows, configs);
    }

    // ==================== QUICK TEST ====================

    private static synchronized void printProgressParallel(int completed, int total, long startTime,
                                                           String instance, BenchmarkConfig config, int seed,
                                                           BenchmarkResult result, Integer referenceRevenue) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        double progress = (double) completed / total;
        long eta = progress > 0 ? (long) (elapsed / progress - elapsed) : 0;

        double gap = referenceRevenue != null && referenceRevenue > 0 ?
                ((double) referenceRevenue - result.revenue) / referenceRevenue * 100 : -1;

        System.out.printf("[%d/%d] (%.1f%%) ETA: %dm %ds%n",
                completed, total, progress * 100, eta / 60, eta % 60);
        System.out.printf("  Instance: %s%n", instance);
        System.out.printf("  Moves: %s (%d)%n", config.moves, config.moves.size());
        System.out.printf("  Config: adaptive=%s, alpha=%s, skip=%.1f%n",
                config.adaptiveMoves, config.alphaType, config.skipProbability);
        System.out.printf("  Result: Revenue=%d, Gap=%.2f%%, Iter/s=%.1f, seed=%d%n",
                result.revenue, gap, result.iterationsPerSecond, seed);
    }

    // ==================== FULL BENCHMARK ====================

    private static BenchmarkResult runSingleBenchmark(
            ProblemParameters problem,
            String instancePath,
            int seed,
            BenchmarkConfig config,
            int timeLimit) throws Exception {

        LocalSearchSettings localSearchSettings = new LocalSearchSettings(
                new ArrayList<>(config.moves),
                config.skipProbability,  // from config
                config.adaptiveMoves,
                true  // always track statistics
        );

        GraspSettings graspSettings = new GraspSettings(
                SearchMode.FIRST_IMPROVEMENT,
                timeLimit,
                localSearchSettings,
                new ConstructiveHeuristicSettings(0.5, 2),
                config.createAlphaGenerator(),  // created from config
                seed,
                instancePath
        );

        GraspWithPathRelinking grasp = new GraspWithPathRelinking(problem, graspSettings);
        SolverSolution solution = grasp.getSolution();

        // Calculate time to best solution
        double timeToBest = 0;
        if (!solution.getCheckPoints().isEmpty()) {
            timeToBest = solution.getCheckPoints().get(solution.getCheckPoints().size() - 1).getTime();
        }

        return new BenchmarkResult(
                solution.getBestSolution().revenue,
                timeToBest,
                solution.getCheckPoints().size(),
                grasp.getIterationsPerSecond(),
                grasp.getMoveStatistics()
        );
    }

    private static String[] formatResultRow(String instance, String config, int seed,
                                            BenchmarkResult result, Integer referenceRevenue) {
        double gap = -1;
        if (referenceRevenue != null && referenceRevenue > 0) {
            gap = ((double) referenceRevenue - result.revenue) / referenceRevenue * 100;
        }

        return new String[]{
                instance,
                config,
                String.valueOf(seed),
                String.valueOf(result.revenue),
                referenceRevenue != null ? String.valueOf(referenceRevenue) : "N/A",
                String.format("%.4f", gap),
                String.format("%.2f", result.timeToBest),
                String.valueOf(result.iterations),
                String.format("%.2f", result.iterationsPerSecond)
        };
    }

    // ==================== SINGLE BENCHMARK RUN ====================

    private static void collectMoveStatRows(String instance, String config, int seed,
                                            MoveStatistics stats, List<String[]> rows) {
        if (stats == null || stats.isEmpty()) return;

        for (String moveName : stats.getMoveNames()) {
            rows.add(new String[]{
                    instance,
                    config,
                    String.valueOf(seed),
                    moveName,
                    String.valueOf(stats.getAttempts(moveName)),
                    String.valueOf(stats.getSuccesses(moveName)),
                    String.format("%.2f", stats.getSuccessRate(moveName)),
                    String.format("%.2f", stats.getAverageGain(moveName)),
                    String.format("%.3f", stats.getAverageTimeMs(moveName))
            });
        }
    }

    // ==================== OUTPUT METHODS ====================

    private static void writeResultsCSV(String path, List<String[]> rows) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("instance,config,seed,revenue,reference_revenue,gap_pct,time_to_best_s,iterations,iter_per_sec");
            for (String[] row : rows) {
                writer.println(String.join(",", row));
            }
        } catch (Exception e) {
            System.err.println("Error writing results CSV: " + e.getMessage());
        }
    }

    private static void writeMoveStatisticsCSV(String path, List<String[]> rows) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("instance,config,seed,move_name,attempts,successes,success_rate_pct,avg_gain,avg_time_ms");
            for (String[] row : rows) {
                writer.println(String.join(",", row));
            }
        } catch (Exception e) {
            System.err.println("Error writing move statistics CSV: " + e.getMessage());
        }
    }

    private static void writeConfigSummary(String path, List<BenchmarkConfig> configs, int timeLimit) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("Benchmark Configuration Summary");
            writer.println("=".repeat(60));
            writer.println();
            writer.println("Execution Parameters:");
            writer.println("  Thread count: " + THREAD_COUNT);
            writer.println("  Wall-clock duration: " + BENCHMARK_DURATION_MINUTES + " min");
            writer.println("  Time limit per run: " + timeLimit + "s");
            writer.println("  Seeds: " + Arrays.toString(Arrays.copyOf(SEEDS, OVERNIGHT_SEEDS)));
            writer.println("  Uniform alpha range: [" + UNIFORM_ALPHA_MIN + ", " + UNIFORM_ALPHA_MAX + "]");
            writer.println();
            writer.println("Move Set Configuration:");
            writer.println("-".repeat(60));
            writer.println("  Base moves: " + BASE_MOVES);
            writer.println("  Number of subsets sampled: " + NUM_MOVE_SUBSETS);
            writer.println("  Minimum moves per subset: " + MIN_MOVES_IN_SUBSET);
            writer.println("  Move subset seed: " + MOVE_SUBSET_SEED);
            writer.println("  Generated move sets (" + MOVE_SET_CONFIGS.size() + "):");
            for (int i = 0; i < MOVE_SET_CONFIGS.size(); i++) {
                writer.println("    " + (i + 1) + ": " + MOVE_SET_CONFIGS.get(i));
            }
            writer.println();
            writer.println("Parameter Space (factorial design):");
            writer.println("-".repeat(60));
            writer.println("  Move sets: " + MOVE_SET_CONFIGS.size() + " configurations");
            writer.println("  Adaptive moves: " + ADAPTIVE_MOVES_OPTIONS + " (" + ADAPTIVE_MOVES_OPTIONS.size() + " options)");
            writer.println("  Alpha types: " + ALPHA_TYPES + " (" + ALPHA_TYPES.size() + " options)");
            writer.println("  Fixed alpha values: " + FIXED_ALPHA_VALUES + " (" + FIXED_ALPHA_VALUES.size() + " options)");
            writer.println("  Skip probabilities: " + SKIP_PROBABILITIES + " (" + SKIP_PROBABILITIES.size() + " options)");
            writer.println();
            writer.println("  Total configurations generated: " + CONFIGS.size());
            writer.println();
            writer.println("Configurations:");
            writer.println("-".repeat(60));

            for (BenchmarkConfig config : configs) {
                writer.println();
                writer.println("  " + config.name);
                writer.println("    Moves: " + config.moves);
                writer.println("    Adaptive moves: " + config.adaptiveMoves);
                writer.println("    Alpha generator: " + config.getAlphaGeneratorIdentifier());
                writer.println("    Skip probability: " + config.skipProbability);
            }

            writer.println();
            writer.println("Priority instances:");
            for (String inst : PRIORITY_INSTANCES) {
                writer.println("  - " + inst);
            }

            writer.println();
            writer.println("Secondary instances:");
            for (String inst : SECONDARY_INSTANCES) {
                writer.println("  - " + inst);
            }

        } catch (Exception e) {
            System.err.println("Error writing config summary: " + e.getMessage());
        }
    }

    private static Map<String, Integer> loadReferenceSolutions() {
        Map<String, Integer> solutions = new HashMap<>();
        String discreteOutputDir = "output/discrete";
        Gson gson = new Gson();

        String[] allInstances = concat(PRIORITY_INSTANCES, SECONDARY_INSTANCES);

        for (String instancePath : allInstances) {
            String instanceName = Paths.get(instancePath).getFileName().toString().replace(".json", "");
            String solutionPath = discreteOutputDir + "/" + instanceName + "/solution.json";

            if (Files.exists(Paths.get(solutionPath))) {
                try (FileReader reader = new FileReader(solutionPath)) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    int revenue = json.getAsJsonObject("bestSolution").get("revenue").getAsInt();
                    solutions.put(instanceName, revenue);
                } catch (Exception e) {
                    // Silently skip invalid reference files
                }
            }
        }

        return solutions;
    }

    private static String[] concat(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    // ==================== HELPER METHODS ====================

    private static void printProgress(int completed, int total, long startTime,
                                      String instance, String config, int seed) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        double progress = (double) completed / total;
        long eta = (long) (elapsed / progress - elapsed);

        System.out.printf("\n[%d/%d] (%.1f%%) ETA: %dm %ds | %s | %s | seed=%d%n",
                completed, total, progress * 100,
                eta / 60, eta % 60,
                instance, config, seed);
    }

    private static void printResult(String config, BenchmarkResult result, Integer referenceRevenue) {
        System.out.printf("  Revenue: %d%n", result.revenue);
        System.out.printf("  Time to best: %.2fs%n", result.timeToBest);
        System.out.printf("  Iterations: %d%n", result.iterations);

        if (referenceRevenue != null && referenceRevenue > 0) {
            double gap = ((double) referenceRevenue - result.revenue) / referenceRevenue * 100;
            System.out.printf("  Gap to reference: %.2f%%%n", gap);
        }
    }

    private static void printConfigGroups(List<BenchmarkConfig> configs) {
        System.out.println("Parameter space:");
        System.out.println("  Adaptive moves: " + ADAPTIVE_MOVES_OPTIONS);
        System.out.println("  Alpha types: " + ALPHA_TYPES);
        System.out.println("  Fixed alpha values: " + FIXED_ALPHA_VALUES);
        System.out.println("  Skip probabilities: " + SKIP_PROBABILITIES);
        System.out.println();
        System.out.println("Move Set Configuration:");
        System.out.println("  Base moves: " + BASE_MOVES);
        System.out.println("  Number of subsets sampled: " + NUM_MOVE_SUBSETS);
        System.out.println("  Minimum moves per subset: " + MIN_MOVES_IN_SUBSET);
        System.out.println("  Move subset seed: " + MOVE_SUBSET_SEED);
        System.out.println("  Generated move sets (" + MOVE_SET_CONFIGS.size() + "):");
        for (int i = 0; i < MOVE_SET_CONFIGS.size(); i++) {
            System.out.println("    " + (i + 1) + ": " + MOVE_SET_CONFIGS.get(i));
        }
        System.out.println();
        System.out.println("Generated configurations (" + configs.size() + " total):");
        for (BenchmarkConfig config : configs) {
            System.out.println("  - " + config.name);
        }
        System.out.println();
    }

    private static void printSummaryStatistics(List<String[]> results, List<BenchmarkConfig> configs) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SUMMARY STATISTICS BY CONFIGURATION");
        System.out.println("=".repeat(70));

        Map<String, List<Double>> gapsByConfig = new LinkedHashMap<>();
        Map<String, List<Integer>> revenuesByConfig = new LinkedHashMap<>();

        for (String[] row : results) {
            String config = row[1];
            int revenue = Integer.parseInt(row[3]);
            double gap = Double.parseDouble(row[5]);

            gapsByConfig.computeIfAbsent(config, k -> new ArrayList<>()).add(gap);
            revenuesByConfig.computeIfAbsent(config, k -> new ArrayList<>()).add(revenue);
        }

        System.out.printf("%-25s %10s %10s %10s %10s%n",
                "Config", "Avg Gap%", "Min Gap%", "Max Gap%", "Avg Revenue");
        System.out.println("-".repeat(70));

        for (BenchmarkConfig config : configs) {
            List<Double> gaps = gapsByConfig.get(config.name);
            List<Integer> revenues = revenuesByConfig.get(config.name);

            if (gaps == null || gaps.isEmpty()) continue;

            double avgGap = gaps.stream().mapToDouble(d -> d).average().orElse(0);
            double minGap = gaps.stream().mapToDouble(d -> d).min().orElse(0);
            double maxGap = gaps.stream().mapToDouble(d -> d).max().orElse(0);
            double avgRevenue = revenues.stream().mapToInt(i -> i).average().orElse(0);

            System.out.printf("%-25s %9.2f%% %9.2f%% %9.2f%% %10.0f%n",
                    config.name, avgGap, minGap, maxGap, avgRevenue);
        }

        System.out.println("=".repeat(70));
    }

    /**
     * Benchmark configuration holder with all parameter values.
     */
    static class BenchmarkConfig {
        final String name;
        final List<String> moves;
        final boolean adaptiveMoves;
        final String alphaType;
        final double fixedAlpha;
        final double skipProbability;

        BenchmarkConfig(String name, List<String> moves, boolean adaptiveMoves,
                        String alphaType, double fixedAlpha, double skipProbability) {
            this.name = name;
            this.moves = moves;
            this.adaptiveMoves = adaptiveMoves;
            this.alphaType = alphaType;
            this.fixedAlpha = fixedAlpha;
            this.skipProbability = skipProbability;
        }

        /**
         * Creates the appropriate AlphaGenerator based on alphaType.
         */
        AlphaGenerator createAlphaGenerator() {
            return switch (alphaType) {
                case "reactive" -> new AlphaGeneratorReactive();
                case "uniform" -> new AlphaGeneratorUniform(UNIFORM_ALPHA_MIN, UNIFORM_ALPHA_MAX);
                case "fixed" -> new AlphaGeneratorConstant(fixedAlpha);
                default -> throw new IllegalArgumentException("Unknown alpha type: " + alphaType);
            };
        }

        /**
         * Returns a string identifier for the alpha generator (for display purposes).
         */
        String getAlphaGeneratorIdentifier() {
            return switch (alphaType) {
                case "reactive" -> "reactive";
                case "uniform" -> String.format("uniform[%.1f,%.1f]", UNIFORM_ALPHA_MIN, UNIFORM_ALPHA_MAX);
                case "fixed" -> String.format("fixed(%.2f)", fixedAlpha);
                default -> alphaType;
            };
        }
    }

    static class BenchmarkResult {
        final int revenue;
        final double timeToBest;
        final int iterations;
        final double iterationsPerSecond;
        final MoveStatistics moveStats;

        BenchmarkResult(int revenue, double timeToBest, int iterations,
                        double iterationsPerSecond, MoveStatistics moveStats) {
            this.revenue = revenue;
            this.timeToBest = timeToBest;
            this.iterations = iterations;
            this.iterationsPerSecond = iterationsPerSecond;
            this.moveStats = moveStats;
        }
    }
}
