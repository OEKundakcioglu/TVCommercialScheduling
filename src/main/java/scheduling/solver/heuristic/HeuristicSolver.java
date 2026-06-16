package scheduling.solver.heuristic;

import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.Solver;
import scheduling.solver.SolverSolution;

@RequiredArgsConstructor
public class HeuristicSolver<T> implements Solver<T> {

    private final HeuristicAlgorithm<T> algorithm;

    @Override
    public SolverSolution<T> solve(Problem problem) {
        return algorithm.run(problem);
    }
}
