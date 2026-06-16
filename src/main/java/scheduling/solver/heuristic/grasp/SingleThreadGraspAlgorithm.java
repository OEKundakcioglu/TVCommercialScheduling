package scheduling.solver.heuristic.grasp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import scheduling.model.Problem;
import scheduling.solver.CheckPoint;
import scheduling.solver.SolverSolution;
import scheduling.solver.heuristic.grasp.construction.ReactiveAlphaGenerator;
import scheduling.solver.heuristic.grasp.elitepool.ElitePool;
import scheduling.solver.heuristic.grasp.vnd.VND;
import scheduling.solver.heuristic.grasp.vnd.statistics.PhaseStatistics;

@Slf4j
public class SingleThreadGraspAlgorithm extends GraspAlgorithm {

    private static final int LOG_INTERVAL = 100;

    private final Random random;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Random is intentionally shared")
    public SingleThreadGraspAlgorithm(GraspConfig config, Random random) {
        super(config);
        this.random = random;
    }

    @Override
    public SolverSolution<GraspInformation> run(Problem problem) {
        var config = getConfig();
        var vnd = new VND(config.getVndConfig(), random);
        var alphaGen = new ReactiveAlphaGenerator();
        var elitePool = new ElitePool(config.getElitePoolSize(), problem.getCommercials().length);
        var constructionStats = new PhaseStatistics();
        var localSearchStats = new PhaseStatistics();
        var pathRelinkingStats = new PhaseStatistics();
        var checkPoints = new ArrayList<CheckPoint>();

        var startTimeMillis = System.currentTimeMillis();

        var best = buildInitialSolution(problem, random);
        elitePool.add(best);
        checkPoints.add(createCheckPoint(best.getTotalRevenue(), startTimeMillis));

        var bestFoundAtSeconds = 0.0;
        var iteration = 0;

        while (elapsedSeconds(startTimeMillis) < config.getTimeLimitSeconds()) {
            iteration++;

            var result =
                    runIteration(
                            problem,
                            elitePool,
                            alphaGen,
                            vnd,
                            random,
                            constructionStats,
                            localSearchStats,
                            pathRelinkingStats);

            if (result.getTotalRevenue() > best.getTotalRevenue()) {
                best = result;
                bestFoundAtSeconds = elapsedSeconds(startTimeMillis);
                checkPoints.add(createCheckPoint(best.getTotalRevenue(), startTimeMillis));
            }

            if (iteration % config.getUpdateInterval() == 0) {
                config.getVndConfig().getSelector().update();
                alphaGen.update();
            }

            if (iteration % LOG_INTERVAL == 0) {
                logProgress(iteration, startTimeMillis, best.getTotalRevenue(), bestFoundAtSeconds);
            }
        }

        var solution = SolutionConverter.toSolution(problem, best);
        var info =
                new GraspInformation(
                        config,
                        vnd.getStatistics(),
                        constructionStats,
                        localSearchStats,
                        pathRelinkingStats);
        return new SolverSolution<>(solution, checkPoints, info);
    }

    private void logProgress(
            int iteration, long startTimeMillis, double bestRevenue, double bestFoundAt) {
        var elapsed = elapsedSeconds(startTimeMillis);
        var rate = iteration / elapsed;
        log.info(
                "Elapsed: {}s | Iteration: {} | Rate: {}/s | Best: {} | Found at: {}s",
                String.format("%.1f", elapsed),
                iteration,
                String.format("%.1f", rate),
                String.format("%.2f", bestRevenue),
                String.format("%.1f", bestFoundAt));
    }
}
