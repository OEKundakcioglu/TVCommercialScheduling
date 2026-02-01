package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;

import runParameters.GraspSettings;

import solvers.CheckPoint;
import solvers.heuristicSolvers.grasp.ComponentEfficacy;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ParallelGraspWithPathRelinking extends BaseGrasp {

    private final int threadCount;
    private final List<Solution> eliteSolutions;
    private final List<CheckPoint> checkPoints;
    private final AtomicInteger iterations = new AtomicInteger(0);
    private final ReentrantReadWriteLock eliteLock = new ReentrantReadWriteLock();
    private final AtomicReference<Solution> bestSolution = new AtomicReference<>();
    private volatile int currentEliteCount = 0;
    private volatile int foundSolutionAt;
    private final MoveStatistics aggregatedMoveStatistics;
    private final ComponentEfficacy aggregatedComponentEfficacy = new ComponentEfficacy();

    public ParallelGraspWithPathRelinking(
            ProblemParameters parameters, GraspSettings graspSettings, int threadCount)
            throws Exception {
        super(parameters, graspSettings);

        this.eliteSolutions = new ArrayList<>();
        this.threadCount =
                threadCount <= 0 ? Runtime.getRuntime().availableProcessors() : threadCount;
        System.out.println(
                "Running with "
                        + this.threadCount
                        + " threads (Available processors: "
                        + Runtime.getRuntime().availableProcessors()
                        + ").");

        this.checkPoints = new ArrayList<>();
        this.aggregatedMoveStatistics = createMoveStatistics();

        this.solve();

        this.solverSolution =
                buildSolverSolution(
                        bestSolution.get(),
                        checkPoints,
                        aggregatedMoveStatistics,
                        aggregatedComponentEfficacy);
    }

    private void solve() throws Exception {
        // Use seed from settings for reproducibility
        long baseSeed = graspSettings.seed();
        Random initialRandom = new Random(baseSeed);

        // Initial solution (single threaded to start)
        this.bestSolution.set(
                createConstructiveSolution(
                        graspSettings.alphaGenerator().generateAlpha(initialRandom),
                        initialRandom));

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

        // Sort checkpoints by time (threads merged in arbitrary order)
        checkPoints.sort(Comparator.comparingDouble(CheckPoint::getTime));

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
        LocalSearch localSearch = createLocalSearch(random, threadLocalStats);

        // Thread-local checkpoint list - merged at end of thread execution
        List<CheckPoint> threadLocalCheckpoints = new ArrayList<>();

        // Thread-local alpha generator to avoid synchronization overhead
        var sharedAlphaGen = this.graspSettings.alphaGenerator();
        AlphaGeneratorReactive threadLocalReactiveAlpha = null;
        if (sharedAlphaGen instanceof AlphaGeneratorReactive reactive) {
            threadLocalReactiveAlpha = reactive.createThreadLocalCopy();
        }

        while (System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {

            // Constructive Phase - with reactive alpha support (thread-local, no synchronization)
            double alpha =
                    (threadLocalReactiveAlpha != null)
                            ? threadLocalReactiveAlpha.generateAlpha(random)
                            : sharedAlphaGen.generateAlpha(random);

            long chStartTime = System.nanoTime();
            var randomSolution =
                    createConstructiveSolution(
                            graspSettings.alphaGenerator().generateAlpha(random), random);
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

            // Path Relinking Phase - use volatile size check first (no lock needed)
            boolean performPathRelinking = false;
            Solution guidingSolution = null;
            Solution initialSolution = randomSolution;

            if (currentEliteCount > 2) {
                // Only acquire read lock when we actually need to get a guiding solution
                eliteLock.readLock().lock();
                try {
                    if (!eliteSolutions.isEmpty()) {
                        performPathRelinking = true;
                        guidingSolution = getGuidingSolution(random);
                    }
                } finally {
                    eliteLock.readLock().unlock();
                }
            }

            if (performPathRelinking) {
                double prPrevRevenue = randomSolution.revenue;
                long prStartTime = System.nanoTime();
                randomSolution = runPathRelinking(initialSolution, guidingSolution, random);
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

            // Provide feedback to thread-local reactive alpha generator (no synchronization needed)
            if (threadLocalReactiveAlpha != null) {
                threadLocalReactiveAlpha.updateFeedback(alpha, randomSolution.revenue);
            }

            // Update Global State
            updateEliteSolutions(randomSolution, startTime, threadLocalCheckpoints);

            int currentTotalIterations = iterations.incrementAndGet();

            // Logging (only one thread usually, or periodically)
            // No synchronization needed - volatile/atomic reads, slight interleaving is acceptable
            if (currentTotalIterations % 100 == 0) {
                double currentIterationsPerSecond =
                        currentTotalIterations
                                / (double) (System.currentTimeMillis() / 1000 - startTime);
                System.out.printf(
                        "Seconds: %d, Total Iterations: %d, Iteration per second: %f, Best solution: %d found at %ds%n",
                        System.currentTimeMillis() / 1000 - startTime,
                        currentTotalIterations,
                        currentIterationsPerSecond,
                        bestSolution.get().revenue,
                        foundSolutionAt);
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

        // Merge thread-local alpha feedback into shared generator
        if (threadLocalReactiveAlpha != null
                && sharedAlphaGen instanceof AlphaGeneratorReactive sharedReactive) {
            synchronized (sharedReactive) {
                sharedReactive.mergeFeedback(threadLocalReactiveAlpha);
            }
        }

        // Merge thread-local checkpoints into shared list
        if (!threadLocalCheckpoints.isEmpty()) {
            synchronized (checkPoints) {
                checkPoints.addAll(threadLocalCheckpoints);
            }
        }
    }

    private void updateEliteSolutions(
            Solution newFoundLocalOptima, long startTime, List<CheckPoint> threadLocalCheckpoints) {
        // Update best solution using lock-free compare-and-swap
        Solution currentBest;
        do {
            currentBest = bestSolution.get();
            if (newFoundLocalOptima.revenue <= currentBest.revenue) {
                break; // Not better, exit CAS loop
            }
        } while (!bestSolution.compareAndSet(currentBest, newFoundLocalOptima));

        // If we successfully updated best solution, record checkpoint (thread-local, no sync
        // needed)
        if (newFoundLocalOptima.revenue > currentBest.revenue) {
            this.foundSolutionAt = (int) (System.currentTimeMillis() / 1000 - startTime);
            threadLocalCheckpoints.add(
                    new CheckPoint(
                            newFoundLocalOptima,
                            ((double) System.currentTimeMillis() / 1000 - startTime)));
        }

        // Update elite solutions pool using write lock
        eliteLock.writeLock().lock();
        try {
            if (tryAddToElitePool(newFoundLocalOptima, eliteSolutions, eliteSolutionsSize)) {
                currentEliteCount = eliteSolutions.size();
            }
        } finally {
            eliteLock.writeLock().unlock();
        }
    }

    @Override
    public MoveStatistics getMoveStatistics() {
        return aggregatedMoveStatistics;
    }

    private Solution getGuidingSolution(java.util.Random random) {
        // Must be called with lock held or handled for concurrent access if
        // eliteSolutions is modified
        // Since we are in synchronized(lock) when calling this loop, it is safe
        return eliteSolutions.get(random.nextInt(eliteSolutions.size()));
    }
}
