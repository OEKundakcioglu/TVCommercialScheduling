package scheduling.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProblemDTOReaderTest {

    @Test
    void readsJsonFile() {
        var path = Path.of("src/test/resources/test_scenario.json");
        var problem = ProblemDTOReader.read(path);

        assertEquals(162, problem.commercials().size());
        assertEquals(18, problem.inventories().size());
        assertEquals(2160, problem.ratings().size());

        var firstCommercial = problem.commercials().getFirst();
        assertEquals(0, firstCommercial.id());
        assertEquals(5423, firstCommercial.group());
        assertEquals(22, firstCommercial.audienceType());
        assertEquals(17, firstCommercial.duration());
        assertEquals("PPR", firstCommercial.pricingType());
        assertFalse(firstCommercial.suitableInventories().isEmpty());

        var firstInventory = problem.inventories().getFirst();
        assertEquals(0, firstInventory.id());
        assertEquals(251, firstInventory.duration());
        assertEquals(1, firstInventory.hour());
        assertEquals(12, firstInventory.maxNumberOfCommercial());

        var firstRating = problem.ratings().getFirst();
        assertEquals(0, firstRating.inventoryId());
        assertEquals(1, firstRating.minute());
        assertEquals(0, firstRating.audienceType());
    }
}
