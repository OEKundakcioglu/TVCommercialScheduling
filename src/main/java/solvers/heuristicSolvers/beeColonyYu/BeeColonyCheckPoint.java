package solvers.heuristicSolvers.beeColonyYu;

import data.Solution;

public class BeeColonyCheckPoint {
    private final BeeColonySolution solution;
    private final double time;

    public BeeColonyCheckPoint(BeeColonySolution solution, double time) {
        this.solution = solution;
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    public BeeColonySolution getSolution() {
        return solution;
    }
}
