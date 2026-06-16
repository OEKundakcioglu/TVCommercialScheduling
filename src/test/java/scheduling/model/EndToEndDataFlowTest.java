package scheduling.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scheduling.mapping.ProblemDTOReader;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class EndToEndDataFlowTest {

    private static Problem problem;

    @BeforeAll
    static void setUp() {
        var dto = ProblemDTOReader.read(Path.of("src/test/resources/end-to-end-test.json"));
        problem = ProblemBuilder.build(dto);
    }

    @Test
    void commercial0Fields() {
        var comm = problem.getCommercials()[0];

        assertEquals(0, comm.getId());
        assertEquals(10, comm.getGroup());
        assertEquals(0, comm.getAudienceType());
        assertEquals(10, comm.getDuration());
        assertEquals(50.0, comm.getPrice(), 1e-10);
        assertEquals(PricingType.PPR, comm.getPricingType());
    }

    @Test
    void commercial1Fields() {
        var comm = problem.getCommercials()[1];

        assertEquals(1, comm.getId());
        assertEquals(20, comm.getGroup());
        assertEquals(1, comm.getAudienceType());
        assertEquals(8, comm.getDuration());
        assertEquals(100.0, comm.getPrice(), 1e-10);
        assertEquals(PricingType.FIXED, comm.getPricingType());
    }

    @Test
    void inventory0Fields() {
        var inv = problem.getInventories()[0];

        assertEquals(0, inv.getId());
        assertEquals(120, inv.getDuration());
        assertEquals(2, inv.getDurationInMinutes());
        assertEquals(1, inv.getHour());
        assertEquals(5, inv.getMaxCommercialCount());
    }

    @Test
    void inventory1Fields() {
        var inv = problem.getInventories()[1];

        assertEquals(1, inv.getId());
        assertEquals(60, inv.getDuration());
        assertEquals(1, inv.getDurationInMinutes());
        assertEquals(2, inv.getHour());
        assertEquals(3, inv.getMaxCommercialCount());
    }

    @Test
    void hours() {
        assertArrayEquals(new int[] {1, 2}, problem.getHours());
    }

    @Test
    void suitabilityMatrix() {
        assertTrue(problem.isSuitable(0, 0));
        assertTrue(problem.isSuitable(0, 1));
        assertTrue(problem.isSuitable(1, 0));
        assertFalse(problem.isSuitable(1, 1));
    }

    @Test
    void attentionTypesForCommercial0Inventory0() {
        var types = problem.getAttentionTypes(0, 0);

        assertEquals(1, types.length);
        assertEquals(AttentionType.N, types[0]);
    }

    @Test
    void attentionTypesForCommercial0Inventory1() {
        var types = problem.getAttentionTypes(0, 1);
        var typeSet = Arrays.asList(types);

        assertEquals(2, types.length);
        assertTrue(typeSet.contains(AttentionType.F12));
        assertTrue(typeSet.contains(AttentionType.L1));
    }

    @Test
    void attentionTypesForCommercial1Inventory0() {
        var types = problem.getAttentionTypes(1, 0);

        assertEquals(1, types.length);
        assertEquals(AttentionType.N, types[0]);
    }

    @Test
    void attentionTypesForUnsuitablePair() {
        var types = problem.getAttentionTypes(1, 1);

        assertEquals(0, types.length);
    }

    @Test
    void suitableInventoriesForCommercial0() {
        var forComm0 = problem.getSuitableInventories(0);

        assertEquals(2, forComm0.length);
        assertTrue(Arrays.stream(forComm0).anyMatch(id -> id == 0));
        assertTrue(Arrays.stream(forComm0).anyMatch(id -> id == 1));
    }

    @Test
    void suitableInventoriesForCommercial1() {
        assertArrayEquals(new int[] {0}, problem.getSuitableInventories(1));
    }

    @Test
    void suitableCommercialsForInventory0() {
        var forInv0 = problem.getSuitableCommercials(0);

        assertEquals(2, forInv0.length);
        assertTrue(Arrays.stream(forInv0).anyMatch(id -> id == 0));
        assertTrue(Arrays.stream(forInv0).anyMatch(id -> id == 1));
    }

    @Test
    void suitableCommercialsForInventory1() {
        assertArrayEquals(new int[] {0}, problem.getSuitableCommercials(1));
    }

    @Test
    void ratingsArray() {
        assertEquals(2.5, problem.getRating(0, 1, 0), 1e-10);
        assertEquals(3.0, problem.getRating(0, 1, 1), 1e-10);
        assertEquals(1.5, problem.getRating(1, 1, 0), 1e-10);
        assertEquals(4.0, problem.getRating(1, 1, 1), 1e-10);
    }

    @Test
    void revenueMatrixPpr() {
        // Commercial 0: PPR, price=50.0, duration=10, audienceType=0
        // Inventory 0, startTime=0: minute=0/60+1=1, rating[0][1][0]=2.5
        // Revenue = 2.5 * 50.0 * 10 = 1250.0
        var revenue = problem.getRevenue(0, 0, 0);
        assertEquals(1250.0, revenue, 1e-10);
    }

    @Test
    void revenueMatrixPprDifferentInventory() {
        // Commercial 0: PPR, price=50.0, duration=10, audienceType=0
        // Inventory 1, startTime=0: minute=0/60+1=1, rating[1][1][0]=1.5
        // Revenue = 1.5 * 50.0 * 10 = 750.0
        var revenue = problem.getRevenue(0, 1, 0);
        assertEquals(750.0, revenue, 1e-10);
    }

    @Test
    void revenueMatrixFixed() {
        // Commercial 1: FIXED, price=100.0, duration=8
        // Revenue = 100.0 * 8 = 800.0 (rating irrelevant)
        var revenue = problem.getRevenue(1, 0, 0);
        assertEquals(800.0, revenue, 1e-10);
    }
}
