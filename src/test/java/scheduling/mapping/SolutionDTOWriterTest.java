package scheduling.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.enums.PricingType;
import scheduling.solver.CheckPoint;
import scheduling.solver.Solution;
import scheduling.solver.SolverSolution;
import scheduling.solver.mip.MipConfig;
import scheduling.solver.mip.MipInformation;
import scheduling.solver.mip.RelaxedMIPConfig;
import scheduling.solver.mip.RelaxedMIPInformation;
import scheduling.solver.mip.RelaxedMIPReturnMode;

class SolutionDTOWriterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void writesJsonFile(@TempDir Path tempDir) throws IOException {
        var inv0 = new Inventory(0, 120, 1, 5);
        var inv1 = new Inventory(1, 60, 2, 3);
        var comm0 = new Commercial(0, 10, 0, 10, 50.0, PricingType.PPR);
        var comm1 = new Commercial(1, 20, 1, 8, 100.0, PricingType.FIXED);

        var assignments = new LinkedHashMap<Inventory, List<Commercial>>();
        assignments.put(inv0, List.of(comm0));
        assignments.put(inv1, List.of(comm1));

        var solution = new Solution(assignments, 1250.0);
        var checkPoints = List.of(new CheckPoint(800.0, 0.5), new CheckPoint(1250.0, 2.3));
        var solverSolution = new SolverSolution<>(solution, checkPoints, "extra-info");

        var outputPath = tempDir.resolve("solution.json");
        SolutionDTOWriter.write(solverSolution, outputPath);

        assertTrue(Files.exists(outputPath));

        var tree = MAPPER.readTree(outputPath.toFile());

        assertEquals(1250.0, tree.get("bestSolution").get("totalRevenue").asDouble(), 1e-10);

        var assignmentsNode = tree.get("bestSolution").get("assignments");
        assertEquals(2, assignmentsNode.size());
        assertEquals(0, assignmentsNode.get(0).get("inventoryId").asInt());
        assertEquals(0, assignmentsNode.get(0).get("commercialIds").get(0).asInt());
        assertEquals(1, assignmentsNode.get(1).get("inventoryId").asInt());
        assertEquals(1, assignmentsNode.get(1).get("commercialIds").get(0).asInt());

        var checkPointsNode = tree.get("checkPoints");
        assertEquals(2, checkPointsNode.size());
        assertEquals(800.0, checkPointsNode.get(0).get("objective").asDouble(), 1e-10);
        assertEquals(0.5, checkPointsNode.get(0).get("time").asDouble(), 1e-10);

        assertEquals("extra-info", tree.get("additionalInformation").asText());
    }

    @Test
    void writesMipInformationAsObject(@TempDir Path tempDir) throws IOException {
        var solution = new Solution(new LinkedHashMap<>(), 0.0);
        var mipInformation =
                new MipInformation(
                        new MipConfig("25", 7200),
                        2,
                        "OPTIMAL",
                        1200.0,
                        1200.0,
                        0.0,
                        15.5,
                        42.0,
                        3);
        var solverSolution = new SolverSolution<>(solution, List.of(), mipInformation);

        var outputPath = tempDir.resolve("solution.json");
        SolutionDTOWriter.write(solverSolution, outputPath);

        var tree = MAPPER.readTree(outputPath.toFile());
        var additionalInformation = tree.get("additionalInformation");

        assertTrue(additionalInformation.isObject());
        assertEquals("25", additionalInformation.get("config").get("instanceName").asText());
        assertEquals(7200, additionalInformation.get("config").get("timeLimitSeconds").asInt());
        assertEquals(2, additionalInformation.get("statusCode").asInt());
        assertEquals("OPTIMAL", additionalInformation.get("status").asText());
        assertEquals(1200.0, additionalInformation.get("objectiveValue").asDouble(), 1e-10);
        assertEquals(1200.0, additionalInformation.get("objectiveBound").asDouble(), 1e-10);
        assertEquals(0.0, additionalInformation.get("mipGap").asDouble(), 1e-10);
        assertEquals(15.5, additionalInformation.get("runtimeSeconds").asDouble(), 1e-10);
        assertEquals(42.0, additionalInformation.get("nodeCount").asDouble(), 1e-10);
        assertEquals(3, additionalInformation.get("solutionCount").asInt());
    }

    @Test
    void writesRelaxedMipInformationAsObject(@TempDir Path tempDir) throws IOException {
        var solution = new Solution(new LinkedHashMap<>(), 900.0);
        var relaxedMipInformation =
                new RelaxedMIPInformation(
                        new RelaxedMIPConfig("25", RelaxedMIPReturnMode.BEST),
                        2,
                        "OPTIMAL",
                        1200.0,
                        1300.0,
                        900.0,
                        0.1,
                        15.5,
                        42.0,
                        3);
        var solverSolution = new SolverSolution<>(solution, List.of(), relaxedMipInformation);

        var outputPath = tempDir.resolve("solution.json");
        SolutionDTOWriter.write(solverSolution, outputPath);

        var tree = MAPPER.readTree(outputPath.toFile());
        var additionalInformation = tree.get("additionalInformation");

        assertTrue(additionalInformation.isObject());
        assertEquals("25", additionalInformation.get("config").get("instanceName").asText());
        assertEquals("BEST", additionalInformation.get("config").get("returnMode").asText());
        assertEquals(1.0e-4, additionalInformation.get("config").get("mipGap").asDouble(), 1e-12);
        assertEquals(2, additionalInformation.get("statusCode").asInt());
        assertEquals("OPTIMAL", additionalInformation.get("status").asText());
        assertEquals(1200.0, additionalInformation.get("relaxedIncumbentValue").asDouble(), 1e-10);
        assertEquals(1300.0, additionalInformation.get("relaxedUpperBound").asDouble(), 1e-10);
        assertEquals(900.0, additionalInformation.get("realizedRevenue").asDouble(), 1e-10);
        assertEquals(0.1, additionalInformation.get("mipGap").asDouble(), 1e-10);
        assertEquals(15.5, additionalInformation.get("runtimeSeconds").asDouble(), 1e-10);
        assertEquals(42.0, additionalInformation.get("nodeCount").asDouble(), 1e-10);
        assertEquals(3, additionalInformation.get("solutionCount").asInt());
    }
}
