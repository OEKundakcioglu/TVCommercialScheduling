package scheduling.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import scheduling.dto.ProblemDTO;
import scheduling.mapping.ProblemDTOReader;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class ProblemBuilderTest {

    private final ProblemDTO dto =
            ProblemDTOReader.read(Path.of("src/test/resources/test_scenario.json"));

    @Test
    void buildsCorrectNumberOfEntities() {
        var problem = ProblemBuilder.build(dto);

        assertEquals(162, problem.getCommercials().length);
        assertEquals(18, problem.getInventories().length);
    }

    @Test
    void mapsInventoryFields() {
        var problem = ProblemBuilder.build(dto);
        var inv0 = problem.getInventories()[0];

        assertEquals(0, inv0.getId());
        assertEquals(251, inv0.getDuration());
        assertEquals(5, inv0.getDurationInMinutes());
        assertEquals(1, inv0.getHour());
        assertEquals(12, inv0.getMaxCommercialCount());
    }

    @Test
    void mapsCommercialFields() {
        var problem = ProblemBuilder.build(dto);
        var comm0 = problem.getCommercials()[0];

        assertEquals(0, comm0.getId());
        assertEquals(5423, comm0.getGroup());
        assertEquals(22, comm0.getAudienceType());
        assertEquals(17, comm0.getDuration());
        assertEquals(88.63625269738301, comm0.getPrice(), 1e-10);
        assertEquals(PricingType.PPR, comm0.getPricingType());
    }

    @Test
    void mapsFixedPricingType() {
        var problem = ProblemBuilder.build(dto);
        var comm15 = problem.getCommercials()[15];

        assertEquals(PricingType.FIXED, comm15.getPricingType());
    }

    @Test
    void extractsDistinctHours() {
        var problem = ProblemBuilder.build(dto);
        var hours = problem.getHours();

        assertEquals(5, hours.length);
    }

    @Test
    void buildsSuitabilityArray() {
        var problem = ProblemBuilder.build(dto);

        // Commercial 0 is suitable for inventories 0,1,3,4,5,7,8,9,10,12,13,14,15,16
        assertTrue(problem.isSuitable(0, 0));
        assertTrue(problem.isSuitable(0, 14));
        assertFalse(problem.isSuitable(0, 2));
    }

    @Test
    void buildsAttentionTypes() {
        var problem = ProblemBuilder.build(dto);

        // Commercial 0: F12 -> [3,4,5,13,7,8,9,14,15,16,10,0,1,12]
        assertArrayEquals(new AttentionType[] {AttentionType.F12}, problem.getAttentionTypes(0, 0));

        // Commercial 33: F12->[4,5,8,9,15,16,10,1], L12->[3,4,7,8,14,15,10,0]
        // Inventory 4 should have both F12 and L12
        var attForComm33Inv4 = problem.getAttentionTypes(33, 4);
        assertEquals(2, attForComm33Inv4.length);
    }

    @Test
    void buildsSuitableInventoriesFor() {
        var problem = ProblemBuilder.build(dto);

        // Commercial 0 has 14 suitable inventories
        assertEquals(14, problem.getSuitableInventories(0).length);
    }

    @Test
    void buildsSuitableCommercialsFor() {
        var problem = ProblemBuilder.build(dto);

        // Inventory 0 has 128 suitable commercials
        assertEquals(128, problem.getSuitableCommercials(0).length);
    }

    @Test
    void buildsRatings() {
        var problem = ProblemBuilder.build(dto);

        // Rating: inventoryId=0, minute=1, audienceType=0, rating=2.1545188475280437
        assertEquals(2.1545188475280437, problem.getRating(0, 1, 0), 1e-10);
    }

    @Test
    void buildsRevenueMatrixForPpr() {
        var problem = ProblemBuilder.build(dto);

        // Commercial 0: PPR, price=88.63625269738301, duration=17, audienceType=22
        // At inventory 0, startTime 0: minute = 0/60+1 = 1
        // Rating at [0][1][22] = 2.0791550195280917
        // Revenue = rating * price * duration
        assertEquals(3132.9046650347123, problem.getRevenue(0, 0, 0), 1e-4);
    }

    @Test
    void buildsRevenueMatrixForFixed() {
        var problem = ProblemBuilder.build(dto);

        // Commercial 15: FIXED, price=505.3821425727979, duration=35
        // Revenue = price * duration = 17688.374990047927
        assertEquals(17688.374990047927, problem.getRevenue(15, 7, 0), 1e-4);
    }

    @Test
    void revenueMatrixZeroForUnsuitableInventory() {
        var problem = ProblemBuilder.build(dto);

        // Commercial 0 is NOT suitable for inventory 2
        assertFalse(problem.isSuitable(0, 2));
    }
}
