package scheduling.solver.mip;

import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.Solver;
import scheduling.solver.SolverSolution;
import scheduling.solver.mip.model.RelaxedMIPModel;

@RequiredArgsConstructor
public class RelaxedMIPSolver implements Solver<RelaxedMIPInformation> {

    private final RelaxedMIPModel model;

    @Override
    public SolverSolution<RelaxedMIPInformation> solve(Problem problem) {
        try (model) {
            model.build(problem);
            model.optimize();
            return model.extractSolution();
        }
    }
}
