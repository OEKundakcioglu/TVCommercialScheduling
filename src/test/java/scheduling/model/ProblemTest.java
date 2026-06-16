package scheduling.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class ProblemTest {

    @Test
    void storesEntitiesAndRelationalArrays() {
        var inv0 = new Inventory(0, 120, 1, 5);
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR);

        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0};
        var hours = new int[] {1};

        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var suitableInventoriesFor = new int[][] {{0}};
        var suitableCommercialsFor = new int[][] {{0}};
        var ratings = new double[][][] {{{2.5}}};
        var revenueMatrix = new double[][][] {{{100.0 * 10 * 2.5}}};

        var problem =
                new Problem(
                        commercials,
                        inventories,
                        hours,
                        suitability,
                        attentionTypes,
                        suitableInventoriesFor,
                        suitableCommercialsFor,
                        ratings,
                        revenueMatrix);

        assertEquals(1, problem.getCommercials().length);
        assertEquals(1, problem.getInventories().length);
        assertArrayEquals(new int[] {1}, problem.getHours());
        assertTrue(problem.isSuitable(0, 0));
        assertEquals(AttentionType.N, problem.getAttentionTypes(0, 0)[0]);
        assertArrayEquals(new int[] {0}, problem.getSuitableInventories(0));
        assertArrayEquals(new int[] {0}, problem.getSuitableCommercials(0));
        assertEquals(2.5, problem.getRating(0, 0, 0), 1e-10);
        assertEquals(2500.0, problem.getRevenue(0, 0, 0), 1e-10);
    }
}
