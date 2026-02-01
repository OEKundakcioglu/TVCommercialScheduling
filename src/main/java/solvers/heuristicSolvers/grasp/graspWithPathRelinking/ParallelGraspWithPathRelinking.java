package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;

import runParameters.GraspSettings;

import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.ComponentEfficacy;
import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;
import solvers.heuristicSolvers.grasp.pathLinking.MixedPathRelinking;
import solvers.heuristicSolvers.grasp.pathLinking.PathRelinkingUtils;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelGraspWithPathRelinking {
    @SuppressWarnings("FieldCanBeLocal")
    private final int eliteSolutionsSize = 10;

    private final int threadCount;

    private final ProblemParameters parameters;
    private final List<Solution> eliteSolutions;
    private final GraspSettings graspSettings;
    private final List<CheckPoint> checkPoints;
    private final SolverSolution solverSolution;
    private final PathRelinkingUtils pathRelinkingUtils;
    private final AtomicInteger iterations = new AtomicInteger(0);
    private final Object lock = new Object();
    private Solution bestSolution;
    private int foundSolutionAt;
    private double iterationsPerSecond;
    private final MoveStatistics aggregatedMoveStatistics;
    private final ComponentEfficacy aggregatedComponentEfficacy = new ComponentEfficacy();

    public ParallelGraspWithPathRelinking(
            ProblemParameters parameters, GraspSettings graspSettings, int threadCount)
            throws Exception {

        this.parameters = parameters;
        this.eliteSolutions = new LinkedList<>();
        this.graspSettings = graspSettings;
        this.threadCount =
                threadCount <= 0 ? Runtime.getRuntime().availableProcessors() : threadCount;
        System.out.println(
                "Running with "
                        + this.threadCount
                        + " threads (Available processors: "
                        + Runtime.getRuntime().availableProcessors()
                        + ").");

        this.checkPoints = new ArrayList<>();
        this.pathRelinkingUtils = new PathRelinkingUtils();
        this.aggregatedMoveStatistics =
                graspSettings.localSearchSettings().trackStatistics ? new MoveStatistics() : null;

        this.solve();

        GraspInformation graspInformation =
                new GraspInformation(
                        graspSettings,
                        iterationsPerSecond,
                        aggregatedMoveStatistics,
                        aggregatedComponentEfficacy);

        this.solverSolution =
                new SolverSolution(
                        bestSolution, checkPoints, graspInformation, parameters.getInstance());
    }

    private void solve() throws Exception {
        // Use seed from settings for reproducibility
        long baseSeed = graspSettings.seed();
        Random initialRandom = new Random(baseSeed);

        // Initial solution (single threaded to start)
        this.bestSolution =
                new ConstructiveHeuristic(
                                parameters,
                                this.graspSettings.alphaGenerator().generateAlpha(initialRandom),
                                graspSettings.constructiveHeuristicSettings(),
                                initialRandom)
                        .getSolution();

        var startTime = System.currentTimeMillis() / 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            // Each thread gets a deterministic seed based on base seed + thread index
            final long threadSeed = baseSeed + i + 1;
            tasks.add(
                    () -> {
                        runGraspLoop(startTime, threadSeed);
                        return null;
                    });
        }

        var features = executor.invokeAll(tasks);
        for (var feature : features) {
            feature.get();
        }
        executor.shutdown();

        var endTime = System.currentTimeMillis() / 1000;
        iterationsPerSecond = iterations.get() / (double) (endTime - startTime);
    }

    private void runGraspLoop(long startTime, long threadSeed) throws Exception {

        var random = new Random(threadSeed);
        // Thread-local statistics - will be merged at the end
        MoveStatistics threadLocalStats =
                (aggregatedMoveStatistics != null) ? new MoveStatistics() : null;
        ComponentEfficacy threadLocalEfficacy = new ComponentEfficacy();

        // Thread-local LocalSearch instance - moveProbabilities persist across iterations within
        // this thread
        LocalSearch localSearch =
                new LocalSearch(
                        parameters,
                        graspSettings.getSearchMode(),
                        graspSettings.localSearchSettings(),
                        random,
                        threadLocalStats);

        while (System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {

            // Constructive Phase - with reactive alpha support
            double alpha;
            var alphaGen = this.graspSettings.alphaGenerator();
            if (alphaGen instanceof AlphaGeneratorReactive reactive) {
                synchronized (reactive) {
                    alpha = reactive.generateAlpha(random);
                }
            } else {
                alpha = alphaGen.generateAlpha(random);
            }

            long chStartTime = System.nanoTime();
            var randomSolution =
                    new ConstructiveHeuristic(
                                    parameters,
                                    this.graspSettings.alphaGenerator().generateAlpha(random),
                                    graspSettings.constructiveHeuristicSettings(),
                                    random)
                            .getSolution();
            threadLocalEfficacy.recordCall(
                    ComponentEfficacy.Component.CONSTRUCTIVE_HEURISTIC,
                    System.nanoTime() - chStartTime,
                    0,
                    randomSolution.revenue);

            // Local Search Phase
            double prevRevenue = randomSolution.revenue;
            long lsStartTime = System.nanoTime();
            randomSolution = localSearch.search(randomSolution);
            threadLocalEfficacy.recordCall(
                    ComponentEfficacy.Component.LOCAL_SEARCH,
                    System.nanoTime() - lsStartTime,
                    prevRevenue,
                    randomSolution.revenue);

            // Path Relinking Phase
            boolean performPathRelinking = false;
            Solution guidingSolution = null;
            Solution initialSolution = randomSolution;

            synchronized (lock) {
                if (this.eliteSolutions.size() > 2) {
                    performPathRelinking = true;
                    guidingSolution = getGuidingSolution(random);
                }
            }

            if (performPathRelinking) {
                double prPrevRevenue = randomSolution.revenue;
                long prStartTime = System.nanoTime();
                randomSolution =
                        new MixedPathRelinking(
                                        parameters,
                                        initialSolution,
                                        guidingSolution,
                                        pathRelinkingUtils,
                                        random)
                                .getBestFoundSolution();
                threadLocalEfficacy.recordCall(
                        ComponentEfficacy.Component.PATH_RELINKING,
                        System.nanoTime() - prStartTime,
                        prPrevRevenue,
                        randomSolution.revenue);

                double lsPrevRevenue = randomSolution.revenue;
                long lsPostPrStartTime = System.nanoTime();
                randomSolution = localSearch.search(randomSolution);
                threadLocalEfficacy.recordCall(
                        ComponentEfficacy.Component.LOCAL_SEARCH,
                        System.nanoTime() - lsPostPrStartTime,
                        lsPrevRevenue,
                        randomSolution.revenue);
            }

            // Provide feedback to reactive alpha generator if applicable
            if (alphaGen instanceof AlphaGeneratorReactive reactive) {
                synchronized (reactive) {
                    reactive.updateFeedback(alpha, randomSolution.revenue);
                }
            }

            // Update Global State
            updateEliteSolutions(randomSolution, startTime);

            int currentTotalIterations = iterations.incrementAndGet();

            // Logging (only one thread usually, or periodically)
            if (currentTotalIterations % 100 == 0) {
                double currentIterationsPerSecond =
                        currentTotalIterations
                                / (double) (System.currentTimeMillis() / 1000 - startTime);
                // Simple logging to avoid spam, synced print
                synchronized (System.out) {
                    System.out.printf(
                            "Seconds: %d, Total Iterations: %d, Iteration per second: %f, Best solution: %d found at %ds%n",
                            System.currentTimeMillis() / 1000 - startTime,
                            currentTotalIterations,
                            currentIterationsPerSecond,
                            bestSolution.revenue,
                            foundSolutionAt);
                }
            }
        }

        // Merge thread-local statistics into aggregated
        if (threadLocalStats != null && aggregatedMoveStatistics != null) {
            synchronized (aggregatedMoveStatistics) {
                aggregatedMoveStatistics.merge(threadLocalStats);
            }
        }

        // Merge thread-local component efficacy into aggregated
        synchronized (aggregatedComponentEfficacy) {
            aggregatedComponentEfficacy.merge(threadLocalEfficacy);
        }
    }

    private boolean updateEliteSolutions(Solution newFoundLocalOptima, long startTime) {

        synchronized (lock) {
            if (newFoundLocalOptima.revenue > bestSolution.revenue) {
                this.foundSolutionAt = (int) (System.currentTimeMillis() / 1000 - startTime);

                bestSolution = newFoundLocalOptima;

                this.checkPoints.add(
                        new CheckPoint(
                                newFoundLocalOptima,
                                ((double) System.currentTimeMillis() / 1000 - startTime)));
            }

            if (eliteSolutions.size() < eliteSolutionsSize) {
                if (eliteSolutions.isEmpty()) {
                    eliteSolutions.add(newFoundLocalOptima);
                    return true;
                } else {
                    var minDistance =
                            eliteSolutions.stream()
                                    .map(
                                            solution ->
                                                    pathRelinkingUtils.distance(
                                                            solution, newFoundLocalOptima))
                                    .min(Comparator.comparing(distance -> distance))
                                    .orElseThrow();
                    if (minDistance > 0) {
                        eliteSolutions.add(newFoundLocalOptima);
                        return true;
                    }
                }
            } else {
                int minDistance =
                        eliteSolutions.stream()
                                .map(
                                        solution ->
                                                pathRelinkingUtils.distance(
                                                        solution, newFoundLocalOptima))
                                .min(Comparator.comparing(distance -> distance))
                                .orElseThrow();

                if (minDistance == 0) return false;

                var worstSolution =
                        eliteSolutions.stream()
                                .min(Comparator.comparing(solution -> solution.revenue))
                                .orElseThrow();
                if (minDistance > 0 && newFoundLocalOptima.revenue > worstSolution.revenue) {
                    eliteSolutions.remove(worstSolution);
                    eliteSolutions.add(newFoundLocalOptima);
                    return true;
                }
            }
            return false;
        }
    }

    public SolverSolution getSolution() {
        return solverSolution;
    }

    /**
     * Get the aggregated move statistics from all local search calls across all threads. Returns
     * null if statistics tracking was not enabled.
     */
    public MoveStatistics getMoveStatistics() {
        return aggregatedMoveStatistics;
    }

    /** Get the iterations per second achieved during the run. */
    public double getIterationsPerSecond() {
        return iterationsPerSecond;
    }

    private Solution getGuidingSolution(java.util.Random random) {
        // Must be called with lock held or handled for concurrent access if
        // eliteSolutions is modified
        // Since we are in synchronized(lock) when calling this loop, it is safe
        return eliteSolutions.get(random.nextInt(eliteSolutions.size()));
    }
}
