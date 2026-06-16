package scheduling.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InventoryDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesFromJson() throws Exception {
        var json =
                """
                {
                    "id": 0,
                    "duration": 311,
                    "hour": 1,
                    "maxNumberOfCommercial": 15
                }
                """;

        var inventory = mapper.readValue(json, InventoryDTO.class);

        assertEquals(0, inventory.id());
        assertEquals(311, inventory.duration());
        assertEquals(1, inventory.hour());
        assertEquals(15, inventory.maxNumberOfCommercial());
    }
}
