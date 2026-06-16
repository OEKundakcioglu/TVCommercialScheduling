package scheduling.solver.heuristic.grasp;

import java.util.Random;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.CheckPoint;
import scheduling.solver.heuristic.HeuristicAlgorithm;
import scheduling.solver.heuristic.grasp.construction.GreedyConstruction;
import scheduling.solver.heuristic.grasp.construction.ReactiveAlphaGenerator;
import scheduling.solver.heuristic.grasp.elitepool.ElitePool;
import scheduling.solver.heuristic.grasp.pathrelinking.MixedPathRelinking;
import scheduling.solver.heuristic.grasp.vnd.VND;
import scheduling.solver.heuristic.grasp.vnd.statistics.PhaseStatistics;

@RequiredArgsConstructor
public abstract class GraspAlgorithm implements HeuristicAlgorithm<GraspInformation> {

    @Getter(AccessLevel.PROTECTED)
    private final GraspConfig config;

    protected GraspSolution buildInitialSolution(Problem problem, Random random) {
        return new GreedyConstruction(
                        problem, 0.0, random, config.getLowerBound(), config.getUpperBound())
                .solve();
    }

    protected GraspSolution runIteration(
            Problem problem,
            ElitePool elitePool,
            ReactiveAlphaGenerator alphaGen,
            VND vnd,
            Random random,
            PhaseStatistics constructionStats,
            PhaseStatistics localSearchStats,
            PhaseStatistics pathRelinkingStats) {
        var alpha = alphaGen.generateAlpha(random);
        var constructed = runConstruction(problem, alpha, random, constructionStats);
        var afterVnd = runLocalSearch(vnd, constructed, localSearchStats);
        alphaGen.feedback(alpha, afterVnd.getTotalRevenue());
        if (elitePool.size() >= 2) {
            var prResult =
                    runPathRelinking(problem, elitePool, random, afterVnd, pathRelinkingStats);
            afterVnd = runLocalSearch(vnd, prResult, localSearchStats);
        }

        elitePool.add(afterVnd);
        return afterVnd;
    }

    protected CheckPoint createCheckPoint(double objective, long startTimeMillis) {
        return new CheckPoint(objective, elapsedSeconds(startTimeMillis));
    }

    protected double elapsedSeconds(long startTimeMillis) {
        return (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    }

    private GraspSolution runConstruction(
            Problem problem, double alpha, Random random, PhaseStatistics constructionStats) {
        var startNanos = System.nanoTime();
        var constructed =
                new GreedyConstruction(
                                problem,
                                alpha,
                                random,
                                config.getLowerBound(),
                                config.getUpperBound())
                        .solve();
        var elapsedNanos = System.nanoTime() - startNanos;
        constructionStats.record(elapsedNanos, constructed.getTotalRevenue());
        return constructed;
    }

    private GraspSolution runLocalSearch(
            VND vnd, GraspSolution constructed, PhaseStatistics localSearchStats) {
        var revenueBefore = constructed.getTotalRevenue();
        var startNanos = System.nanoTime();
        var afterVnd = vnd.search(constructed);
        var elapsedNanos = System.nanoTime() - startNanos;
        localSearchStats.record(elapsedNanos, afterVnd.getTotalRevenue() - revenueBefore);
        return afterVnd;
    }

    private GraspSolution runPathRelinking(
            Problem problem,
            ElitePool elitePool,
            Random random,
            GraspSolution afterVnd,
            PhaseStatistics pathRelinkingStats) {
        var revenueBefore = afterVnd.getTotalRevenue();
        var startNanos = System.nanoTime();
        var guide = elitePool.getRandomGuide(random);
        var prResult = new MixedPathRelinking(problem, afterVnd, guide, random).relink();
        var elapsedNanos = System.nanoTime() - startNanos;
        pathRelinkingStats.record(elapsedNanos, prResult.getTotalRevenue() - revenueBefore);
        return prResult;
    }
}
