package scheduling.solver;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SolverSolution<T> {

    private final Solution bestSolution;
    private final List<CheckPoint> checkPoints;
    private final T additionalInformation;
}
