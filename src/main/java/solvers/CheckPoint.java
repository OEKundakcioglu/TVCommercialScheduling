package solvers;

import data.Solution;

public class CheckPoint {
    private final Solution solution;
    private final double time;

    public CheckPoint(Solution solution, double time) {
        this.solution = solution;
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    public Solution getSolution() {
        return solution;
    }
}
