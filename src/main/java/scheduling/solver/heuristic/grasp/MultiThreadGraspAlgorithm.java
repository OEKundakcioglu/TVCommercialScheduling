package scheduling.solver.heuristic.grasp;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import scheduling.model.Problem;
import scheduling.solver.CheckPoint;
import scheduling.solver.SolverSolution;
import scheduling.solver.heuristic.grasp.construction.ReactiveAlphaGenerator;
import scheduling.solver.heuristic.grasp.elitepool.ElitePool;
import scheduling.solver.heuristic.grasp.vnd.VND;
import scheduling.solver.heuristic.grasp.vnd.statistics.PhaseStatistics;
import scheduling.solver.heuristic.grasp.vnd.statistics.SearchStatistics;

@Slf4j
public class MultiThreadGraspAlgorithm extends GraspAlgorithm {

    private static final int LOG_INTERVAL = 100;

    private final Random random;
    private final int threadCount;

    private record ThreadResult(
            Optional<GraspSolution> best,
            SearchStatistics searchStatistics,
            PhaseStatistics constructionStatistics,
            PhaseStatistics localSearchStatistics,
            PhaseStatistics pathRelinkingStatistics) {}

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Random is intentionally shared")
    public MultiThreadGraspAlgorithm(GraspConfig config, Random random, int threadCount) {
        super(config);
        Objects.requireNonNull(random);
        checkArgument(threadCount > 0, "threadCount must be positive");
        this.random = random;
        this.threadCount = threadCount;
    }

    public MultiThreadGraspAlgorithm(GraspConfig config, Random random) {
        this(config, random, Runtime.getRuntime().availableProcessors());
    }

    @Override
    public SolverSolution<GraspInformation> run(Problem problem) {
        var config = getConfig();
        var elitePool =
                ElitePool.threadSafe(config.getElitePoolSize(), problem.getCommercials().length);

        var startTimeMillis = System.currentTimeMillis();

        var initial = buildInitialSolution(problem, random);
        elitePool.add(initial);

        var checkPoints =
                Collections.synchronizedList(
                        new ArrayList<>(
                                List.of(
                                        createCheckPoint(
                                                initial.getTotalRevenue(), startTimeMillis))));
        var totalIterations = new AtomicInteger(0);
        var baseSeed = random.nextLong();

        log.info("Starting multi-threaded GRASP with {} threads", threadCount);
        var executor = Executors.newFixedThreadPool(threadCount);
        var futures =
                submitTasks(
                        executor,
                        problem,
                        elitePool,
                        totalIterations,
                        checkPoints,
                        startTimeMillis,
                        baseSeed);
        executor.shutdown();
        awaitTermination(executor, config.getTimeLimitSeconds() + 60);

        var best = initial;
        var aggregateSearch = new SearchStatistics();
        var aggregateConstruction = new PhaseStatistics();
        var aggregateLocalSearch = new PhaseStatistics();
        var aggregatePathRelinking = new PhaseStatistics();

        for (var future : futures) {
            try {
                var result = future.get();
                if (result.best().isPresent()
                        && result.best().get().getTotalRevenue() > best.getTotalRevenue()) {
                    best = result.best().get();
                }
                aggregateSearch.merge(result.searchStatistics());
                aggregateConstruction.merge(result.constructionStatistics());
                aggregateLocalSearch.merge(result.localSearchStatistics());
                aggregatePathRelinking.merge(result.pathRelinkingStatistics());
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        var sortedCheckPoints = new ArrayList<>(checkPoints);
        sortedCheckPoints.sort(Comparator.comparingDouble(CheckPoint::getTime));

        var solution = SolutionConverter.toSolution(problem, best);
        var info =
                new GraspInformation(
                        config,
                        aggregateSearch,
                        aggregateConstruction,
                        aggregateLocalSearch,
                        aggregatePathRelinking);
        return new SolverSolution<>(solution, sortedCheckPoints, info);
    }

    private List<Future<ThreadResult>> submitTasks(
            ExecutorService executor,
            Problem problem,
            ElitePool elitePool,
            AtomicInteger totalIterations,
            List<CheckPoint> checkPoints,
            long startTimeMillis,
            long baseSeed) {
        var futures = new ArrayList<Future<ThreadResult>>();
        for (int t = 0; t < threadCount; t++) {
            var seed = baseSeed + t + 1;
            futures.add(
                    executor.submit(
                            () ->
                                    runGraspLoop(
                                            problem,
                                            elitePool,
                                            totalIterations,
                                            checkPoints,
                                            startTimeMillis,
                                            seed)));
        }
        return futures;
    }

    private ThreadResult runGraspLoop(
            Problem problem,
            ElitePool elitePool,
            AtomicInteger totalIterations,
            List<CheckPoint> checkPoints,
            long startTimeMillis,
            long seed) {
        var config = getConfig();
        var threadRandom = new Random(seed);
        var alphaGen = new ReactiveAlphaGenerator();
        var threadVndConfig = config.getVndConfig().withFreshSelector();
        var vnd = new VND(threadVndConfig, threadRandom);
        var constructionStats = new PhaseStatistics();
        var localSearchStats = new PhaseStatistics();
        var pathRelinkingStats = new PhaseStatistics();

        GraspSolution threadBest = null;
        var localIteration = 0;

        while (elapsedSeconds(startTimeMillis) < config.getTimeLimitSeconds()) {
            var result =
                    runIteration(
                            problem,
                            elitePool,
                            alphaGen,
                            vnd,
                            threadRandom,
                            constructionStats,
                            localSearchStats,
                            pathRelinkingStats);

            var iteration = totalIterations.incrementAndGet();

            if (threadBest == null || result.getTotalRevenue() > threadBest.getTotalRevenue()) {
                threadBest = result;
                checkPoints.add(createCheckPoint(result.getTotalRevenue(), startTimeMillis));
            }

            localIteration++;
            if (localIteration % config.getUpdateInterval() == 0) {
                threadVndConfig.getSelector().update();
                alphaGen.update();
            }

            if (iteration % LOG_INTERVAL == 0) {
                logProgress(iteration, startTimeMillis, threadBest.getTotalRevenue());
            }
        }

        return new ThreadResult(
                Optional.ofNullable(threadBest),
                vnd.getStatistics(),
                constructionStats,
                localSearchStats,
                pathRelinkingStats);
    }

    private void logProgress(int iteration, long startTimeMillis, double bestRevenue) {
        var elapsed = elapsedSeconds(startTimeMillis);
        var rate = iteration / elapsed;
        log.info(
                "Elapsed: {}s | Iteration: {} | Rate: {}/s | Best: {}",
                String.format("%.1f", elapsed),
                iteration,
                String.format("%.1f", rate),
                String.format("%.2f", bestRevenue));
    }

    private void awaitTermination(ExecutorService executor, long timeoutSeconds) {
        try {
            executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
