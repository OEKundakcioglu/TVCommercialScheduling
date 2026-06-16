package scheduling.solver;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CheckPoint {

    private final double objective;
    private final double time;
}
