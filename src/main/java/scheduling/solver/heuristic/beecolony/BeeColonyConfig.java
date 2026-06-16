package scheduling.solver.heuristic.beecolony;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import scheduling.solver.RunInfo;

public record BeeColonyConfig(
        RunInfo runInfo,
        int populationSize,
        double initialTemperature,
        double coolingCoefficient,
        int coolingInterval,
        int timeLimitSeconds) {

    public String stringDesc() {
        return "BeeColony["
                + runInfo.stringDesc()
                + ", pop="
                + populationSize
                + ", temp="
                + initialTemperature
                + ", cool="
                + coolingCoefficient
                + ", interval="
                + coolingInterval
                + ", time="
                + timeLimitSeconds
                + "]";
    }

    public Path outputPath(Path baseDir) {
        var hash = hashStringDesc(stringDesc());
        return baseDir.resolve(runInfo.getInstanceName()).resolve(hash);
    }

    private static String hashStringDesc(String desc) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(desc.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
    }
}
