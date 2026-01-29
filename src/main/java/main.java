import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class main {

    // Benchmark instances from the plan
    private static final String[] BENCHMARK_INSTANCES = {
            // Small instances
            "instances/density=LOW_nInventory=10_nHours=3_seed=1.json",
            "instances/density=MEDIUM_nInventory=10_nHours=3_seed=1.json",
            "instances/density=HIGH_nInventory=10_nHours=3_seed=1.json",
            // Medium instances
            "instances/density=LOW_nInventory=15_nHours=4_seed=1.json",
            "instances/density=MEDIUM_nInventory=15_nHours=4_seed=1.json",
            "instances/density=HIGH_nInventory=15_nHours=4_seed=1.json",
            // Large instances
            "instances/density=LOW_nInventory=20_nHours=5_seed=1.json",
            "instances/density=MEDIUM_nInventory=20_nHours=5_seed=1.json",
            "instances/density=HIGH_nInventory=20_nHours=5_seed=1.json",
            // Legacy instance
            "instances/3.json"
    };

    private static final List<String> MOVES = List.of(
            "insert", "outOfPool", "interSwap", "shift", "transfer", "intraSwap"
    );

    private static final int TIME_LIMIT = 30; // seconds per run
    private static final int NUM_SEEDS = 5;   // number of random seeds per instance

    public static void main(String[] args) throws Exception {
//        if (args.length > 0 && args[0].equals("--benchmark")) {
//        runBenchmarks();
//        } else if (args.length > 0 && args[0].equals("--quick-test")) {
        runQuickTest();
//        } else {
//            runSingleInstance();
//        }
    }

    /**
     * Run benchmarks comparing baseline vs improved GRASP against reference solutions.
     */
    private static void runBenchmarks() throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputDir = "benchmark_results/" + timestamp;
        Files.createDirectories(Paths.get(outputDir));

        // Load reference solutions from output/discrete
        Map<String, Integer> referenceSolutions = loadReferenceSolutions();

        String csvFile = outputDir + "/results.csv";
        try (PrintWriter csv = new PrintWriter(new FileWriter(csvFile))) {
            // Write CSV header
            csv.println("instance,config,seed,revenue,reference_revenue,gap_pct,time_to_best_s,iterations");

            for (String instancePath : BENCHMARK_INSTANCES) {
                if (!Files.exists(Paths.get(instancePath))) {
                    System.out.println("Skipping missing instance: " + instancePath);
                    continue;
                }

                String instanceName = Paths.get(instancePath).getFileName().toString().replace(".json", "");
                System.out.println("\n========== Instance: " + instanceName + " ==========");

                // Get reference revenue for this instance
                Integer referenceRevenue = referenceSolutions.get(instanceName);
                if (referenceRevenue != null) {
                    System.out.println("Reference solution revenue: " + referenceRevenue);
                } else {
                    System.out.println("No reference solution found for: " + instanceName);
                }

                ProblemParameters problem = new JsonParser().readData(instancePath);

                for (int seed = 0; seed < NUM_SEEDS; seed++) {
                    // Run baseline configuration
                    System.out.println("\n--- Baseline (seed=" + seed + ") ---");
                    BenchmarkResult baseline = runWithConfig(problem, instancePath, seed, false);
                    csv.println(formatCsvRow(instanceName, "baseline", seed, baseline, referenceRevenue));
                    csv.flush();

                    // Run improved configuration
                    System.out.println("\n--- Improved (seed=" + seed + ") ---");
                    BenchmarkResult improved = runWithConfig(problem, instancePath, seed, true);
                    csv.println(formatCsvRow(instanceName, "improved", seed, improved, referenceRevenue));
                    csv.flush();

                    // Print comparison
                    double improvement = ((double) improved.revenue - baseline.revenue) / baseline.revenue * 100;
                    System.out.printf("Baseline vs Improved: %.2f%% (%d -> %d)%n",
                            improvement, baseline.revenue, improved.revenue);

                    if (referenceRevenue != null) {
                        double baselineGap = ((double) referenceRevenue - baseline.revenue) / referenceRevenue * 100;
                        double improvedGap = ((double) referenceRevenue - improved.revenue) / referenceRevenue * 100;
                        System.out.printf("Gap to reference - Baseline: %.2f%%, Improved: %.2f%%%n",
                                baselineGap, improvedGap);
                    }
                }
            }
        }

        System.out.println("\n\nBenchmark complete! Results saved to: " + csvFile);
    }

    /**
     * Load reference solutions from output/discrete directory.
     */
    private static Map<String, Integer> loadReferenceSolutions() {
        Map<String, Integer> solutions = new HashMap<>();
        String discreteOutputDir = "output/discrete";
        Gson gson = new Gson();

        for (String instancePath : BENCHMARK_INSTANCES) {
            String instanceName = Paths.get(instancePath).getFileName().toString().replace(".json", "");
            String solutionPath = discreteOutputDir + "/" + instanceName + "/solution.json";

            if (Files.exists(Paths.get(solutionPath))) {
                try (FileReader reader = new FileReader(solutionPath)) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    int revenue = json.getAsJsonObject("bestSolution").get("revenue").getAsInt();
                    solutions.put(instanceName, revenue);
                    System.out.println("Loaded reference for " + instanceName + ": " + revenue);
                } catch (Exception e) {
                    System.err.println("Error loading reference solution for " + instanceName + ": " + e.getMessage());
                }
            } else {
                System.out.println("No reference solution at: " + solutionPath);
            }
        }

        return solutions;
    }

    /**
     * Quick test with a single instance to verify the implementation works.
     */
    private static void runQuickTest() throws Exception {
        String instancePath = "instances/density=HIGH_nInventory=15_nHours=4_seed=1.json";

        if (!Files.exists(Paths.get(instancePath))) {
            System.out.println("Test instance not found: " + instancePath);
            return;
        }

        System.out.println("Quick test with: " + instancePath);
        String instanceName = Paths.get(instancePath).getFileName().toString().replace(".json", "");

        // Load reference solution
        Integer referenceRevenue = loadReferenceSolution(instanceName);
        if (referenceRevenue != null) {
            System.out.println("Reference solution revenue: " + referenceRevenue);
        }

        ProblemParameters problem = new JsonParser().readData(instancePath);

        System.out.println("\n--- Improved ---");
        BenchmarkResult improved = runWithConfig(problem, instancePath, 0, true);
        System.out.println("\n--- Baseline ---");
        BenchmarkResult baseline = runWithConfig(problem, instancePath, 0, false);


        System.out.println("\n=== Results ===");
        System.out.printf("Baseline: revenue=%d%n", baseline.revenue);
        System.out.printf("Improved: revenue=%d%n", improved.revenue);

        double improvement = ((double) improved.revenue - baseline.revenue) / baseline.revenue * 100;
        System.out.printf("Baseline vs Improved: %.2f%%%n", improvement);

        if (referenceRevenue != null) {
            double baselineGap = ((double) referenceRevenue - baseline.revenue) / referenceRevenue * 100;
            double improvedGap = ((double) referenceRevenue - improved.revenue) / referenceRevenue * 100;
            System.out.printf("Reference: revenue=%d%n", referenceRevenue);
            System.out.printf("Gap to reference - Baseline: %.2f%%, Improved: %.2f%%%n",
                    baselineGap, improvedGap);
        }
    }

    /**
     * Load a single reference solution from output/discrete directory.
     */
    private static Integer loadReferenceSolution(String instanceName) {
        String solutionPath = "output/discrete/" + instanceName + "/solution.json";
        Gson gson = new Gson();

        if (Files.exists(Paths.get(solutionPath))) {
            try (FileReader reader = new FileReader(solutionPath)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                return json.getAsJsonObject("bestSolution").get("revenue").getAsInt();
            } catch (Exception e) {
                System.err.println("Error loading reference solution: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Run a single instance (original behavior).
     */
    private static void runSingleInstance() throws Exception {
        String instancePath = "instances/density=HIGH_nInventory=15_nHours=4_seed=1.json";
        ProblemParameters problem = new JsonParser().readData(instancePath);

        GraspSettings graspConfig = new GraspSettings(
                SearchMode.FIRST_IMPROVEMENT,
                TIME_LIMIT,
                new LocalSearchSettings(new ArrayList<>(MOVES), 0.5, true), // adaptive moves enabled
                new ConstructiveHeuristicSettings(0.5, 2),
                new AlphaGeneratorReactive(), // reactive alpha
                0,
                instancePath
        );

        GraspWithPathRelinking grasp = new GraspWithPathRelinking(problem, graspConfig);
        SolverSolution solution = grasp.getSolution();

        System.out.println("\nFinal revenue: " + solution.getBestSolution().revenue);
        Utils.feasibilityCheck(solution.getBestSolution());
    }

    /**
     * Run GRASP with specified configuration.
     */
    private static BenchmarkResult runWithConfig(ProblemParameters problem, String instancePath,
                                                 int seed, boolean useImprovements) throws Exception {
        AlphaGenerator alphaGenerator;
        LocalSearchSettings localSearchSettings;

        if (useImprovements) {
            // Improved configuration: reactive alpha, adaptive moves
            alphaGenerator = new AlphaGeneratorReactive();
            localSearchSettings = new LocalSearchSettings(new ArrayList<>(MOVES), 0.1, true);
        } else {
            // Baseline configuration: uniform alpha, fixed moves
            alphaGenerator = new AlphaGeneratorUniform(0.1, 0.9);
            localSearchSettings = new LocalSearchSettings(new ArrayList<>(MOVES), 0.1, false);
        }

        GraspSettings config = new GraspSettings(
                SearchMode.FIRST_IMPROVEMENT,
                TIME_LIMIT,
                localSearchSettings,
                new ConstructiveHeuristicSettings(0.5, 2),
                alphaGenerator,
                seed,
                instancePath
        );

        long startTime = System.currentTimeMillis();
        GraspWithPathRelinking grasp = new GraspWithPathRelinking(problem, config);
        long endTime = System.currentTimeMillis();

        SolverSolution solution = grasp.getSolution();

        // Calculate time to best solution from checkpoints
        double timeToBest = 0;
        if (!solution.getCheckPoints().isEmpty()) {
            timeToBest = solution.getCheckPoints().get(solution.getCheckPoints().size() - 1).getTime();
        }

        return new BenchmarkResult(
                solution.getBestSolution().revenue,
                timeToBest,
                solution.getCheckPoints().size()
        );
    }

    private static String formatCsvRow(String instance, String config, int seed, BenchmarkResult result, Integer referenceRevenue) {
        double gap = -1;
        if (referenceRevenue != null && referenceRevenue > 0) {
            gap = ((double) referenceRevenue - result.revenue) / referenceRevenue * 100;
        }
        return String.format("%s,%s,%d,%d,%s,%.4f,%.2f,%d",
                instance, config, seed, result.revenue,
                referenceRevenue != null ? referenceRevenue.toString() : "N/A",
                gap,
                result.timeToBest,
                result.iterations);
    }

    /**
     * Result holder for benchmark runs.
     */
    private static class BenchmarkResult {
        final int revenue;
        final double timeToBest;
        final int iterations;

        BenchmarkResult(int revenue, double timeToBest, int iterations) {
            this.revenue = revenue;
            this.timeToBest = timeToBest;
            this.iterations = iterations;
        }
    }
}
