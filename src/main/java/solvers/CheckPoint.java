package solvers;

public class CheckPoint {
    private final double objective;
    private final double time;

    public CheckPoint(double objective, double time) {
        this.objective = objective;
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    public double getObjective() {
        return objective;
    }
}
