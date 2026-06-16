package scheduling.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProblemDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesFromJson() throws Exception {
        var json =
                """
                {
                    "commercials": [
                        {
                            "id": 0,
                            "group": 861,
                            "audienceType": 0,
                            "duration": 10,
                            "price": 110.28,
                            "pricingType": "PPR",
                            "suitableInventories": { "N": [0, 1] }
                        }
                    ],
                    "inventories": [
                        {
                            "id": 0,
                            "duration": 311,
                            "hour": 1,
                            "maxNumberOfCommercial": 15
                        }
                    ],
                    "ratings": [
                        {
                            "inventoryId": 0,
                            "minute": 1,
                            "rating": 2.101,
                            "audienceType": 0
                        }
                    ]
                }
                """;

        var problem = mapper.readValue(json, ProblemDTO.class);

        assertEquals(1, problem.commercials().size());
        assertEquals(1, problem.inventories().size());
        assertEquals(1, problem.ratings().size());
        assertEquals(0, problem.commercials().getFirst().id());
        assertEquals(311, problem.inventories().getFirst().duration());
        assertEquals(0, problem.ratings().getFirst().inventoryId());
    }
}
