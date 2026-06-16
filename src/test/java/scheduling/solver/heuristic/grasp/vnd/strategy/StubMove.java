package scheduling.solver.heuristic.grasp.vnd.strategy;

import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;

public class StubMove extends Move {

    private final boolean feasible;
    private final double revenueGain;

    public StubMove(boolean feasible, double revenueGain) {
        super(null, null);
        this.feasible = feasible;
        this.revenueGain = revenueGain;
    }

    @Override
    public boolean checkFeasibility() {
        return feasible;
    }

    @Override
    protected double computeRevenueGain() {
        return revenueGain;
    }

    @Override
    public GraspSolution apply() {
        throw new UnsupportedOperationException();
    }
}
