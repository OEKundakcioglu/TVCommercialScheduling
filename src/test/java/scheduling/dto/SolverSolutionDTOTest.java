package scheduling.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SolverSolutionDTOTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void serializesToExpectedJson() throws JsonProcessingException {
        var assignment = new AssignmentDTO(0, List.of(3, 7, 12));
        var solutionDTO = new SolutionDTO(826371.73, List.of(assignment));
        var checkPoint = new CheckPointDTO(685701.19, 0.29);
        var dto = new SolverSolutionDTO<>(solutionDTO, List.of(checkPoint), "mip-info");

        var json = MAPPER.writeValueAsString(dto);
        var tree = MAPPER.readTree(json);

        assertEquals(826371.73, tree.get("bestSolution").get("totalRevenue").asDouble(), 1e-10);
        assertEquals(
                0, tree.get("bestSolution").get("assignments").get(0).get("inventoryId").asInt());
        assertEquals(
                3,
                tree.get("bestSolution")
                        .get("assignments")
                        .get(0)
                        .get("commercialIds")
                        .get(0)
                        .asInt());
        assertEquals(685701.19, tree.get("checkPoints").get(0).get("objective").asDouble(), 1e-10);
        assertEquals(0.29, tree.get("checkPoints").get(0).get("time").asDouble(), 1e-10);
        assertEquals("mip-info", tree.get("additionalInformation").asText());
    }

    @Test
    void deserializesFromJson() throws JsonProcessingException {
        var json =
                """
                {
                  "bestSolution": {
                    "totalRevenue": 500.0,
                    "assignments": [
                      { "inventoryId": 1, "commercialIds": [2, 4] }
                    ]
                  },
                  "checkPoints": [
                    { "objective": 500.0, "time": 1.0 }
                  ],
                  "additionalInformation": "test"
                }
                """;

        SolverSolutionDTO<String> dto =
                MAPPER.readValue(
                        json,
                        MAPPER.getTypeFactory()
                                .constructParametricType(SolverSolutionDTO.class, String.class));

        assertEquals(500.0, dto.bestSolution().totalRevenue(), 1e-10);
        assertEquals(1, dto.bestSolution().assignments().size());
        assertEquals(1, dto.bestSolution().assignments().getFirst().inventoryId());
        assertEquals(List.of(2, 4), dto.bestSolution().assignments().getFirst().commercialIds());
        assertEquals(1, dto.checkPoints().size());
        assertEquals(500.0, dto.checkPoints().getFirst().objective(), 1e-10);
        assertEquals("test", dto.additionalInformation());
    }
}
