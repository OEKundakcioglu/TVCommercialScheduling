package solvers;

import data.Solution;

public class CheckPoint {
    private final Solution solution;
    private final double objective;
    private final double time;

    // Existing constructor (for heuristic solvers)
    public CheckPoint(Solution solution, double time) {
        this.solution = solution;
        this.objective = solution != null ? solution.revenue : 0;
        this.time = time;
    }

    // New constructor for MIP callback (objective only)
    public CheckPoint(double objective, double time) {
        this.solution = null;
        this.objective = objective;
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    public Solution getSolution() {
        return solution;
    }

    public double getObjective() {
        return objective;
    }
}
