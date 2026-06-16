package scheduling.solver.heuristic.grasp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.InsertNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;

class MultiThreadGraspAlgorithmTest {

    private static final Commercial[] COMMERCIALS =
            new Commercial[] {
                new Commercial(0, 0, 0, 30, 10.0, PricingType.FIXED),
                new Commercial(1, 1, 0, 30, 15.0, PricingType.FIXED),
            };

    private Problem buildSmallProblem() {
        var revenueMatrix = new double[2][1][120];
        for (int c = 0; c < 2; c++) {
            for (int t = 0; t < 120; t++) {
                revenueMatrix[c][0][t] = COMMERCIALS[c].getPrice();
            }
        }
        var ratings = new double[1][2][1];
        ratings[0][0][0] = 1.0;
        ratings[0][1][0] = 1.0;
        return new Problem(
                COMMERCIALS,
                new Inventory[] {new Inventory(0, 120, 1, 4)},
                new int[] {1},
                new boolean[][] {{true}, {true}},
                new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                new int[][] {{0}, {0}},
                new int[][] {{0, 1}},
                ratings,
                revenueMatrix);
    }

    private VNDConfig buildVndConfig(Problem problem) {
        List<Neighborhood> neighborhoods = List.of(new InsertNeighborhood(problem));
        return new VNDConfig(
                new FirstImprovingStrategy(), neighborhoods, new SequentialSelector(), 0.0);
    }

    private GraspConfig buildConfig(Problem problem) {
        var vndConfig = buildVndConfig(problem);
        return new GraspConfig(new RunInfo("test", 0), 1, 5, vndConfig, 0.99, 1.01, 100);
    }

    @Test
    void run_returnsSolverSolutionWithNonNullSolutionAndNonNegativeRevenue() {
        var problem = buildSmallProblem();
        var config = buildConfig(problem);
        var algorithm = new MultiThreadGraspAlgorithm(config, new Random(42), 2);

        var result = algorithm.run(problem);

        assertNotNull(result);
        assertNotNull(result.getBestSolution());
        assertTrue(result.getBestSolution().getTotalRevenue() >= 0.0);
    }

    @Test
    void run_returnsNonEmptyCheckpoints() {
        var problem = buildSmallProblem();
        var config = buildConfig(problem);
        var algorithm = new MultiThreadGraspAlgorithm(config, new Random(42), 2);

        var result = algorithm.run(problem);

        assertNotNull(result.getCheckPoints());
        assertFalse(result.getCheckPoints().isEmpty());
    }

    @Test
    void run_checkpointsAreSortedByTime() {
        var problem = buildSmallProblem();
        var config = buildConfig(problem);
        var algorithm = new MultiThreadGraspAlgorithm(config, new Random(42), 2);

        var result = algorithm.run(problem);

        var checkPoints = result.getCheckPoints();
        for (int i = 1; i < checkPoints.size(); i++) {
            assertTrue(
                    checkPoints.get(i).getTime() >= checkPoints.get(i - 1).getTime(),
                    "Checkpoints should be sorted by time");
        }
    }

    @Test
    void run_defaultThreadCountConstructorWorks() {
        var problem = buildSmallProblem();
        var config = buildConfig(problem);
        var algorithm = new MultiThreadGraspAlgorithm(config, new Random(42));

        var result = algorithm.run(problem);

        assertNotNull(result);
        assertNotNull(result.getBestSolution());
        assertTrue(result.getBestSolution().getTotalRevenue() >= 0.0);
    }
}
