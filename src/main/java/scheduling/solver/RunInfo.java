package scheduling.solver;

import java.util.Objects;
import lombok.Getter;

@Getter
public class RunInfo {

    private final String instanceName;
    private final int seed;

    public RunInfo(String instanceName, int seed) {
        this.instanceName = Objects.requireNonNull(instanceName);
        this.seed = seed;
    }

    public String stringDesc() {
        return "Run[seed=" + seed + "]";
    }
}
