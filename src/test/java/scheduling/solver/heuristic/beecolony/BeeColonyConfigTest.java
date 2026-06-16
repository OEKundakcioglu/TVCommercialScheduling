package scheduling.solver.heuristic.beecolony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import scheduling.solver.RunInfo;

class BeeColonyConfigTest {

    private static final RunInfo TEST_RUN_INFO = new RunInfo("test", 0);

    @Test
    void storesAllParameters() {
        var config = new BeeColonyConfig(TEST_RUN_INFO, 20, 0.02, 0.95, 100, 60);
        assertEquals(20, config.populationSize());
        assertEquals(0.02, config.initialTemperature());
        assertEquals(0.95, config.coolingCoefficient());
        assertEquals(100, config.coolingInterval());
        assertEquals(60, config.timeLimitSeconds());
    }

    @Test
    void stringDescAssemblesAllComponents() {
        var config = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 60);
        var desc = config.stringDesc();
        assertTrue(desc.contains("BeeColony["));
        assertTrue(desc.contains("Run[seed=0]"));
        assertTrue(desc.contains("pop=50"));
        assertTrue(desc.contains("temp=1.0"));
        assertTrue(desc.contains("cool=0.99"));
        assertTrue(desc.contains("interval=10"));
        assertTrue(desc.contains("time=60"));
    }

    @Test
    void outputPathUsesInstanceNameAndHash() {
        var config = new BeeColonyConfig(new RunInfo("inst1", 0), 50, 1.0, 0.99, 10, 60);
        var path = config.outputPath(Path.of("output"));

        assertEquals("inst1", path.getParent().getFileName().toString());
        assertEquals(Path.of("output"), path.getParent().getParent());
        assertTrue(path.getFileName().toString().matches("[0-9a-f]+"));
    }

    @Test
    void outputPathDeterministic() {
        var config1 = new BeeColonyConfig(new RunInfo("inst1", 0), 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(new RunInfo("inst1", 0), 50, 1.0, 0.99, 10, 60);
        assertEquals(config1.outputPath(Path.of("out")), config2.outputPath(Path.of("out")));
    }

    @Test
    void outputPathDiffersForDifferentSeeds() {
        var config1 = new BeeColonyConfig(new RunInfo("inst1", 0), 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(new RunInfo("inst1", 1), 50, 1.0, 0.99, 10, 60);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentPopulationSize() {
        var config1 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(TEST_RUN_INFO, 100, 1.0, 0.99, 10, 60);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentTemperature() {
        var config1 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(TEST_RUN_INFO, 50, 2.0, 0.99, 10, 60);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentCoolingCoefficient() {
        var config1 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.95, 10, 60);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentCoolingInterval() {
        var config1 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 20, 60);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentTimeLimit() {
        var config1 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 60);
        var config2 = new BeeColonyConfig(TEST_RUN_INFO, 50, 1.0, 0.99, 10, 120);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }
}
