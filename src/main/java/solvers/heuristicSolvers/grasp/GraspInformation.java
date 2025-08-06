package solvers.heuristicSolvers.grasp;

import data.Commercial;
import data.Inventory;
import runParameters.GraspSettings;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class GraspInformation {
    private final List<Commercial> commercials;
    private final List<Inventory> inventories;
    private final GraspSettings settings;

    public GraspInformation(List<Commercial> commercials, List<Inventory> inventories, GraspSettings settings) {
        this.commercials = commercials;
        this.inventories = inventories;
        this.settings = settings;
    }
}
