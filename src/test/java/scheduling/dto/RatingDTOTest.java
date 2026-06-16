package scheduling.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RatingDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesFromJson() throws Exception {
        var json =
                """
                {
                    "inventoryId": 0,
                    "minute": 1,
                    "rating": 2.1011929342483926,
                    "audienceType": 0
                }
                """;

        var rating = mapper.readValue(json, RatingDTO.class);

        assertEquals(0, rating.inventoryId());
        assertEquals(1, rating.minute());
        assertEquals(2.1011929342483926, rating.rating(), 1e-10);
        assertEquals(0, rating.audienceType());
    }
}
