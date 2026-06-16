package scheduling.solver.heuristic.beecolony.vrp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;

@Getter
@RequiredArgsConstructor
public class VrpProblem {

    private final Customer[] customers;
    private final Vehicle[] vehicles;
    private final Depot depot;
    private final double[][][] distance;
    private final Problem problem;
}
