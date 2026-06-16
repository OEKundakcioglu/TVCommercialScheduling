package scheduling.solver.mip;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record RelaxedMIPConfig(
        String instanceName, RelaxedMIPReturnMode returnMode, double mipGap) {
    public static final double DEFAULT_MIP_GAP = 1.0e-4;

    public RelaxedMIPConfig(String instanceName, RelaxedMIPReturnMode returnMode) {
        this(instanceName, returnMode, DEFAULT_MIP_GAP);
    }

    public RelaxedMIPConfig {
        checkArgument(mipGap > 0, "mipGap must be positive");
    }

    public String stringDesc() {
        return "RelaxedMIP[return=" + returnMode + ",gap=" + mipGap + "]";
    }

    public Path outputPath(Path baseDir) {
        var hash = hashStringDesc(stringDesc());
        return baseDir.resolve(instanceName).resolve(hash);
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
