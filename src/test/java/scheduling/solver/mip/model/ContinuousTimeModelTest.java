package scheduling.solver.mip.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class ContinuousTimeModelTest {

    @Test
    void calculatesRevenueFromExtractedAssignmentStartTimes() {
        var comm0 = new Commercial(0, 1, 0, 45, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 30, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 2);
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[120];
        revenueMatrix[1][0] = new double[120];
        revenueMatrix[0][0][0] = 1000.0;
        revenueMatrix[1][0][45] = 2500.0;
        var problem =
                new Problem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0},
                        new int[] {1},
                        new boolean[][] {{true}, {true}},
                        new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                        new int[][] {{0}, {0}},
                        new int[][] {{0, 1}},
                        new double[][][] {{{0.0}}},
                        revenueMatrix);

        var revenue =
                ContinuousTimeModel.calculateRevenue(problem, Map.of(inv0, List.of(comm0, comm1)));

        assertEquals(3500.0, revenue, 1e-10);
    }
}
