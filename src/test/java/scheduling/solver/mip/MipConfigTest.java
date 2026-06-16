package scheduling.solver.mip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MipConfigTest {

    @Test
    void storesAllParameters() {
        var config = new MipConfig("inst1", 300);
        assertEquals("inst1", config.instanceName());
        assertEquals(300, config.timeLimitSeconds());
    }

    @Test
    void nonPositiveTimeLimitThrows() {
        assertThrows(IllegalArgumentException.class, () -> new MipConfig("inst1", 0));
        assertThrows(IllegalArgumentException.class, () -> new MipConfig("inst1", -1));
    }

    @Test
    void stringDescIncludesTimeLimit() {
        var config = new MipConfig("inst1", 300);
        assertEquals("MIP[time=300]", config.stringDesc());
    }

    @Test
    void outputPathUsesInstanceNameAndHash() {
        var config = new MipConfig("inst1", 300);
        var path = config.outputPath(Path.of("output"));

        assertEquals("inst1", path.getParent().getFileName().toString());
        assertEquals(Path.of("output"), path.getParent().getParent());
        assertTrue(path.getFileName().toString().matches("[0-9a-f]+"));
    }

    @Test
    void outputPathDeterministic() {
        var config1 = new MipConfig("inst1", 300);
        var config2 = new MipConfig("inst1", 300);
        assertEquals(config1.outputPath(Path.of("out")), config2.outputPath(Path.of("out")));
    }

    @Test
    void outputPathDiffersForDifferentTimeLimit() {
        var config1 = new MipConfig("inst1", 300);
        var config2 = new MipConfig("inst1", 600);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentInstance() {
        var config1 = new MipConfig("inst1", 300);
        var config2 = new MipConfig("inst2", 300);
        assertNotEquals(config1.outputPath(Path.of("out")), config2.outputPath(Path.of("out")));
    }
}
