package scheduling.solver;

import scheduling.model.Problem;

public interface Solver<T> {

    SolverSolution<T> solve(Problem problem);
}
