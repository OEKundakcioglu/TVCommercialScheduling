package scheduling.solver.heuristic.beecolony;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BeeColonySolution {
    private final int[] solutionString;
    private final double fitness;
}
