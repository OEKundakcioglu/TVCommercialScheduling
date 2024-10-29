package runParameters;

import java.util.List;

public record MipRunSettings(
        List<Integer> checkPointTimes,
        String logPath
) {
}
