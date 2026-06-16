package scheduling.solver.heuristic.grasp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;

class GraspConfigTest {

    private static final RunInfo TEST_RUN_INFO = new RunInfo("test", 0);

    private VNDConfig buildVndConfig() {
        return new VNDConfig(
                new FirstImprovingStrategy(), List.of(), new SequentialSelector(), 0.0);
    }

    @Test
    void allFieldsStoredCorrectly() {
        var vndConfig = buildVndConfig();
        var config = new GraspConfig(TEST_RUN_INFO, 60, 10, vndConfig, 0.8, 1.2, 100);

        assertEquals(TEST_RUN_INFO, config.getRunInfo());
        assertEquals(60, config.getTimeLimitSeconds());
        assertEquals(10, config.getElitePoolSize());
        assertEquals(vndConfig, config.getVndConfig());
        assertEquals(0.8, config.getLowerBound());
        assertEquals(1.2, config.getUpperBound());
        assertEquals(100, config.getUpdateInterval());
    }

    @Test
    void nonPositiveTimeLimitSecondsThrows() {
        var vndConfig = buildVndConfig();
        assertThrows(
                IllegalArgumentException.class,
                () -> new GraspConfig(TEST_RUN_INFO, 0, 10, vndConfig, 0.8, 1.2, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GraspConfig(TEST_RUN_INFO, -1, 10, vndConfig, 0.8, 1.2, 100));
    }

    @Test
    void nonPositiveElitePoolSizeThrows() {
        var vndConfig = buildVndConfig();
        assertThrows(
                IllegalArgumentException.class,
                () -> new GraspConfig(TEST_RUN_INFO, 60, 0, vndConfig, 0.8, 1.2, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GraspConfig(TEST_RUN_INFO, 60, -1, vndConfig, 0.8, 1.2, 100));
    }

    @Test
    void nullVndConfigThrows() {
        assertThrows(
                NullPointerException.class,
                () -> new GraspConfig(TEST_RUN_INFO, 60, 10, null, 0.8, 1.2, 100));
    }

    @Test
    void nonPositiveUpdateIntervalThrows() {
        var vndConfig = buildVndConfig();
        assertThrows(
                IllegalArgumentException.class,
                () -> new GraspConfig(TEST_RUN_INFO, 60, 10, vndConfig, 0.8, 1.2, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GraspConfig(TEST_RUN_INFO, 60, 10, vndConfig, 0.8, 1.2, -1));
    }

    @Test
    void stringDescAssemblesAllComponents() {
        var vndConfig = buildVndConfig();
        var config = new GraspConfig(TEST_RUN_INFO, 60, 10, vndConfig, 0.8, 1.2, 100);
        var expected =
                "GRASP[Run[seed=0], elitePool=10, alpha=[0.8,1.2], upd=100,"
                        + " VND[FIRST, SEQ, skip=0.0]]";
        assertEquals(expected, config.stringDesc());
    }

    @Test
    void outputPathUsesInstanceNameAndHash() {
        var vndConfig = buildVndConfig();
        var runInfo = new RunInfo("inst1", 0);
        var config = new GraspConfig(runInfo, 60, 10, vndConfig, 0.8, 1.2, 100);
        var path = config.outputPath(Path.of("output"));

        assertEquals("inst1", path.getParent().getFileName().toString());
        assertEquals(Path.of("output"), path.getParent().getParent());
        assertTrue(path.getFileName().toString().matches("[0-9a-f]+"));
    }

    @Test
    void outputPathDeterministic() {
        var vndConfig = buildVndConfig();
        var config1 = new GraspConfig(new RunInfo("inst1", 0), 60, 10, vndConfig, 0.8, 1.2, 100);
        var config2 = new GraspConfig(new RunInfo("inst1", 0), 60, 10, vndConfig, 0.8, 1.2, 100);
        assertEquals(config1.outputPath(Path.of("out")), config2.outputPath(Path.of("out")));
    }

    @Test
    void outputPathDiffersForDifferentSeeds() {
        var vndConfig = buildVndConfig();
        var config1 = new GraspConfig(new RunInfo("inst1", 0), 60, 10, vndConfig, 0.8, 1.2, 100);
        var config2 = new GraspConfig(new RunInfo("inst1", 1), 60, 10, vndConfig, 0.8, 1.2, 100);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentConfigs() {
        var vndConfig = buildVndConfig();
        var config1 = new GraspConfig(new RunInfo("inst1", 0), 60, 10, vndConfig, 0.8, 1.2, 100);
        var config2 = new GraspConfig(new RunInfo("inst1", 0), 60, 5, vndConfig, 0.8, 1.2, 100);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }
}
