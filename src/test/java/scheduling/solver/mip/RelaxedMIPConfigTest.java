package scheduling.solver.mip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RelaxedMIPConfigTest {

    @Test
    void storesAllParameters() {
        var config = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST);
        assertEquals("inst1", config.instanceName());
        assertEquals(RelaxedMIPReturnMode.BEST, config.returnMode());
        assertEquals(1.0e-4, config.mipGap(), 1e-12);
    }

    @Test
    void nonPositiveMipGapThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST, -1));
    }

    @Test
    void stringDescIncludesReturnModeAndMipGap() {
        var config = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.AVERAGE);
        assertEquals("RelaxedMIP[return=AVERAGE,gap=1.0E-4]", config.stringDesc());
    }

    @Test
    void outputPathUsesInstanceNameAndHash() {
        var config = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST);
        var path = config.outputPath(Path.of("output"));
        var instancePath = path.getParent();
        assertNotNull(instancePath);

        assertEquals("inst1", instancePath.getFileName().toString());
        assertEquals(Path.of("output"), instancePath.getParent());
        assertTrue(path.getFileName().toString().matches("[0-9a-f]+"));
    }

    @Test
    void outputPathDeterministic() {
        var config1 = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST);
        var config2 = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST);
        assertEquals(config1.outputPath(Path.of("out")), config2.outputPath(Path.of("out")));
    }

    @Test
    void outputPathDiffersForDifferentMipGap() {
        var config1 = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST, 1.0e-4);
        var config2 = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST, 1.0e-5);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersForDifferentReturnMode() {
        var config1 = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST);
        var config2 = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.WORST);
        assertNotEquals(
                config1.outputPath(Path.of("out")).getFileName(),
                config2.outputPath(Path.of("out")).getFileName());
    }

    @Test
    void outputPathDiffersFromContinuousMIPConfig() {
        var relaxedConfig = new RelaxedMIPConfig("inst1", RelaxedMIPReturnMode.BEST);
        var continuousConfig = new MipConfig("inst1", 300);

        assertNotEquals(
                continuousConfig.outputPath(Path.of("out")),
                relaxedConfig.outputPath(Path.of("out")));
    }
}
