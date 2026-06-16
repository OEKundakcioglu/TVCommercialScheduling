package scheduling.solver.mip.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class RelaxedMIPObjectiveTest {

    @Test
    void bestRevenueInBreakUsesHighestMinuteRevenue() {
        var problem =
                createSingleCommercialProblem(
                        new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR),
                        new double[] {1000.0, 2500.0, 1500.0});

        assertEquals(2500.0, RelaxedMIPObjective.bestRevenueInBreak(problem, 0, 0), 1e-10);
    }

    @Test
    void averageRevenueInBreakUsesMeanMinuteRevenue() {
        var problem =
                createSingleCommercialProblem(
                        new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR),
                        new double[] {1000.0, 2500.0, 1500.0});

        assertEquals(5000.0 / 3.0, RelaxedMIPObjective.averageRevenueInBreak(problem, 0, 0), 1e-10);
    }

    @Test
    void worstRevenueInBreakUsesLowestMinuteRevenue() {
        var problem =
                createSingleCommercialProblem(
                        new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR),
                        new double[] {1000.0, 2500.0, 1500.0});

        assertEquals(1000.0, RelaxedMIPObjective.worstRevenueInBreak(problem, 0, 0), 1e-10);
    }

    @Test
    void bestRevenueInBreakHandlesFixedPriceRevenue() {
        var problem =
                createSingleCommercialProblem(
                        new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                        new double[] {1000.0, 1000.0});

        assertEquals(1000.0, RelaxedMIPObjective.bestRevenueInBreak(problem, 0, 0), 1e-10);
    }

    private static Problem createSingleCommercialProblem(
            Commercial commercial, double[] minuteRevenues) {
        var inventory = new Inventory(0, minuteRevenues.length * 60, 1, 1);
        var revenueMatrix = new double[1][1][];
        revenueMatrix[0][0] = new double[inventory.getDuration()];
        for (int minute = 0; minute < minuteRevenues.length; minute++) {
            revenueMatrix[0][0][minute * 60] = minuteRevenues[minute];
        }

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
