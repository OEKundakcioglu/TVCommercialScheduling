package scheduling.solver.mip.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.mip.RelaxedMIPConfig;
import scheduling.solver.mip.RelaxedMIPReturnMode;
import scheduling.solver.mip.RelaxedMIPSolver;

class RelaxedMIPModelSmokeTest {

    @Test
    void solvesTinyRelaxedModelWhenGurobiAvailable() {
        assumeTrue(gurobiAvailable());

        var problem = createTinyProblem();
        var config = new RelaxedMIPConfig("tiny", RelaxedMIPReturnMode.BEST);
        var solver = new RelaxedMIPSolver(new RelaxedMIPModel(config));

        var result = solver.solve(problem);

        FeasibilityCheck.check(problem, result.getBestSolution());
        var info = result.getAdditionalInformation();
        assertEquals(1000.0, result.getBestSolution().getTotalRevenue(), 1e-10);
        assertEquals(2500.0, info.relaxedIncumbentValue(), 1e-10);
        assertEquals(2500.0, info.relaxedUpperBound(), 1e-10);
        assertEquals(1000.0, info.realizedRevenue(), 1e-10);
    }

    private static boolean gurobiAvailable() {
        try {
            var env = new GRBEnv(true);
            env.set(GRB.IntParam.OutputFlag, 0);
            env.start();
            env.dispose();
            return true;
        } catch (GRBException | UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static Problem createTinyProblem() {
        var commercial = new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR);
        var inventory = new Inventory(0, 120, 1, 1);
        var revenueMatrix = new double[1][1][];
        revenueMatrix[0][0] = new double[120];
        revenueMatrix[0][0][0] = 1000.0;
        revenueMatrix[0][0][60] = 2500.0;

        return new Problem(
                new Commercial[] {commercial},
                new Inventory[] {inventory},
                new int[] {1},
                new boolean[][] {{true}},
                new AttentionType[][][] {{{AttentionType.N}}},
                new int[][] {{0}},
                new int[][] {{0}},
                new double[][][] {{{0.0}}},
                revenueMatrix);
    }
}
