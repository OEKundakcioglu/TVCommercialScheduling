package solvers.heuristicSolvers.grasp;

import data.Commercial;
import data.Inventory;
import runParameters.GraspSettings;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class GraspInformation {
    private final GraspSettings settings;
    private final double iterationsPerSecond;

    public GraspInformation(GraspSettings settings, double iterationsPerSecond) {
        this.settings = settings;
        this.iterationsPerSecond = iterationsPerSecond;
    }
}
