package scheduling.solver.heuristic;

import scheduling.model.Problem;
import scheduling.solver.SolverSolution;

public interface HeuristicAlgorithm<T> {

    SolverSolution<T> run(Problem problem);
}
