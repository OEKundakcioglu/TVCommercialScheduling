package scheduling.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommercialDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesFromJson() throws Exception {
        var json =
                """
                {
                    "id": 5,
                    "group": 5271,
                    "audienceType": 0,
                    "duration": 27,
                    "price": 112.733,
                    "pricingType": "PPR",
                    "suitableInventories": {
                        "F2": [7, 3, 14, 0]
                    }
                }
                """;

        var commercial = mapper.readValue(json, CommercialDTO.class);

        assertEquals(5, commercial.id());
        assertEquals(5271, commercial.group());
        assertEquals(0, commercial.audienceType());
        assertEquals(27, commercial.duration());
        assertEquals(112.733, commercial.price(), 1e-10);
        assertEquals("PPR", commercial.pricingType());
        assertEquals(Map.of("F2", List.of(7, 3, 14, 0)), commercial.suitableInventories());
    }
}
