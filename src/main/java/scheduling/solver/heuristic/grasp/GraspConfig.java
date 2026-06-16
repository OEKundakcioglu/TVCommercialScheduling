package scheduling.solver.heuristic.grasp;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import lombok.Getter;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;

@Getter
public class GraspConfig {

    private final RunInfo runInfo;
    private final int timeLimitSeconds;
    private final int elitePoolSize;
    private final VNDConfig vndConfig;
    private final double lowerBound;
    private final double upperBound;
    private final int updateInterval;

    public GraspConfig(
            RunInfo runInfo,
            int timeLimitSeconds,
            int elitePoolSize,
            VNDConfig vndConfig,
            double lowerBound,
            double upperBound,
            int updateInterval) {
        this.runInfo = Objects.requireNonNull(runInfo);
        checkArgument(timeLimitSeconds > 0, "timeLimitSeconds must be positive");
        checkArgument(elitePoolSize > 0, "elitePoolSize must be positive");
        checkArgument(updateInterval > 0, "updateInterval must be positive");
        this.timeLimitSeconds = timeLimitSeconds;
        this.elitePoolSize = elitePoolSize;
        this.vndConfig = Objects.requireNonNull(vndConfig);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.updateInterval = updateInterval;
    }

    public String stringDesc() {
        return "GRASP["
                + runInfo.stringDesc()
                + ", elitePool="
                + elitePoolSize
                + ", alpha=["
                + lowerBound
                + ","
                + upperBound
                + "]"
                + ", upd="
                + updateInterval
                + ", "
                + vndConfig.stringDesc()
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
