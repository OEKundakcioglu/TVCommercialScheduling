package scheduling.solver.heuristic.grasp.vnd;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Random;
import lombok.Getter;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.vnd.statistics.SearchStatistics;

public class VND {

    private final VNDConfig config;
    private final Random random;

    @Getter private final SearchStatistics statistics = new SearchStatistics();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Random is intentionally shared")
    public VND(VNDConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    public GraspSolution search(GraspSolution initial) {
        var current = initial;
        var noImprovementCount = 0;
        var neighborhoodCount = config.getNeighborhoods().size();

        while (noImprovementCount < neighborhoodCount) {
            statistics.recordIteration();
            var neighborhood = config.getSelector().select(config.getNeighborhoods(), random);

            if (random.nextDouble() < config.getNeighborhoodSkipProbability()) {
                noImprovementCount++;
                continue;
            }

            var moveStats = statistics.getOrCreateMoveStatistics(neighborhood.type());

            var startTime = System.nanoTime();
            var candidates = neighborhood.generateMoves(current, random);
            var selectedMove = config.getStrategy().selectMove(candidates);
            var elapsed = System.nanoTime() - startTime;

            if (selectedMove.isPresent()) {
                var move = selectedMove.get();
                var gain = move.calculateRevenueGain();
                current = move.apply();
                moveStats.recordAttempt(elapsed);
                moveStats.recordSuccess(gain);
                statistics.recordImprovement();
                config.getSelector().reportResult(neighborhood, gain);
                noImprovementCount = 0;
            } else {
                moveStats.recordAttempt(elapsed);
                config.getSelector().reportResult(neighborhood, 0.0);
                noImprovementCount++;
            }
        }

        return current;
    }
}
