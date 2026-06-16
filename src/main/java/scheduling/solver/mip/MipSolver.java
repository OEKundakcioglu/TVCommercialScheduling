package scheduling.solver.mip;

import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.Solver;
import scheduling.solver.SolverSolution;

@RequiredArgsConstructor
public class MipSolver implements Solver<MipInformation> {

    private final MipModel model;

    @Override
    public SolverSolution<MipInformation> solve(Problem problem) {
        try (model) {
            model.build(problem);
            model.optimize();
            return model.extractSolution();
        }
    }
}
