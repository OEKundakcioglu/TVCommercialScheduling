package solvers.mipSolvers;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBException;

import solvers.CheckPoint;

import java.util.ArrayList;
import java.util.List;

public class MipCallback extends GRBCallback {
    private final List<CheckPoint> checkPoints = new ArrayList<>();
    private double bestObjective = Double.NEGATIVE_INFINITY;

    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIPSOL) {
                double objective = getDoubleInfo(GRB.CB_MIPSOL_OBJ);

                // Only record if strictly improving (maximization)
                if (objective > bestObjective) {
                    bestObjective = objective;
                    double runtime = getDoubleInfo(GRB.CB_RUNTIME);
                    checkPoints.add(new CheckPoint(objective, runtime));
                }
            }
        } catch (GRBException e) {
            System.err.println("Callback error: " + e.getMessage());
        }
    }

    public List<CheckPoint> getCheckPoints() {
        return checkPoints;
    }
}
