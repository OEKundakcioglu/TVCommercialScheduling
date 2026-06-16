package scheduling.solver.mip.model;

import java.util.ArrayList;
import java.util.List;
import scheduling.solver.CheckPoint;

class MipCheckpointRecorder {

    private static final double IMPROVEMENT_EPSILON = 1e-6;

    private final List<CheckPoint> checkPoints = new ArrayList<>();
    private double bestObjective = Double.NEGATIVE_INFINITY;

    void record(double objective, double time) {
        if (objective <= bestObjective + IMPROVEMENT_EPSILON) {
            return;
        }

        bestObjective = objective;
        checkPoints.add(new CheckPoint(objective, time));
    }

    List<CheckPoint> snapshot() {
        return List.copyOf(checkPoints);
    }
}
