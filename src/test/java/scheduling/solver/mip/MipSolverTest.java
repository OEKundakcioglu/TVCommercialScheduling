package scheduling.solver.mip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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

class MipSolverTest {

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
    void orchestratesBuildOptimizeExtractClose() {
        var callOrder = new ArrayList<String>();
        var expectedSolution =
                new SolverSolution<>(
                        new Solution(Map.of(), 0.0), List.of(), createMipInformation());

        var fakeModel =
                new MipModel() {
                    @Override
                    public void build(Problem problem) {
                        callOrder.add("build");
                    }

                    @Override
                    public void optimize() {
                        callOrder.add("optimize");
                    }

                    @Override
                    public SolverSolution<MipInformation> extractSolution() {
                        callOrder.add("extractSolution");
                        return expectedSolution;
                    }

                    @Override
                    public void close() {
                        callOrder.add("close");
                    }
                };

        var solver = new MipSolver(fakeModel);
        var result = solver.solve(createDummyProblem());

        assertSame(expectedSolution, result);
        assertEquals(List.of("build", "optimize", "extractSolution", "close"), callOrder);
    }

    @Test
    void closesModelEvenOnBuildException() {
        var closed = new boolean[] {false};

        var failingModel =
                new MipModel() {
                    @Override
                    public void build(Problem problem) {
                        throw new RuntimeException("build failed");
                    }

                    @Override
                    public void optimize() {}

                    @Override
                    public SolverSolution<MipInformation> extractSolution() {
                        return null;
                    }

                    @Override
                    public void close() {
                        closed[0] = true;
                    }
                };

        var solver = new MipSolver(failingModel);
        assertThrows(RuntimeException.class, () -> solver.solve(createDummyProblem()));
        assertTrue(closed[0]);
    }

    private static MipInformation createMipInformation() {
        return new MipInformation(
                new MipConfig("test", 300), 2, "OPTIMAL", 0.0, 0.0, 0.0, 0.0, 0.0, 1);
    }
}
