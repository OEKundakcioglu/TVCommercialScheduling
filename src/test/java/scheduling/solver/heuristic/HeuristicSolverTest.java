package scheduling.solver.heuristic;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.Solution;
import scheduling.solver.SolverSolution;

class HeuristicSolverTest {

    private static Problem createDummyProblem() {
        return new Problem(
                new Commercial[] {new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR)},
                new Inventory[] {new Inventory(0, 120, 1, 5)},
                new int[] {1},
                new boolean[][] {{true}},
                new AttentionType[][][] {{{AttentionType.N}}},
                new int[][] {{0}},
                new int[][] {{0}},
                new double[][][] {{{2.5}}},
                new double[][][] {{{2500.0}}});
    }

    @Test
    void delegatesToAlgorithm() {
        var expectedSolution =
                new SolverSolution<>(new Solution(Map.of(), 0.0), List.of(), "grasp-info");

        HeuristicAlgorithm<String> fakeAlgorithm = problem -> expectedSolution;

        var solver = new HeuristicSolver<>(fakeAlgorithm);
        var result = solver.solve(createDummyProblem());

        assertSame(expectedSolution, result);
    }
}
