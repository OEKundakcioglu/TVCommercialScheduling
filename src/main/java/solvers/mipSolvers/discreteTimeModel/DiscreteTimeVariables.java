package solvers.mipSolvers.discreteTimeModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import data.Commercial;
import data.Inventory;
import data.ProblemParameters;

import java.util.List;
import java.util.Map;

public class DiscreteTimeVariables {

    private final GRBModel model;
    private final GRBVar[][][] X;
    private final Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints;
    private final ProblemParameters parameters;

    public DiscreteTimeVariables(
            Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints,
            ProblemParameters parameters,
            GRBModel model)
            throws GRBException {
        this.X =
                new GRBVar[parameters.getSetOfCommercials().size()]
                        [parameters.getSetOfInventories().size()]
                        [parameters.getSetOfInventories().stream().mapToInt(Inventory::getDuration).max().orElseThrow() + 10];
        this.feasibleTimePoints = feasibleTimePoints;
        this.parameters = parameters;
        this.model = model;

        populateX();
    }

    private void populateX() throws GRBException {
        for (var commercial : parameters.getSetOfCommercials()) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                for (var timePoint : feasibleTimePoints.get(commercial).get(inventory)) {
                    var variable =
                            model.addVar(
                                    0,
                                    1,
                                    0,
                                    GRB.BINARY,
                                    "x_"
                                            + commercial.getId()
                                            + "_"
                                            + inventory.getId()
                                            + "_"
                                            + timePoint);
                    X[commercial.getId()][inventory.getId()][timePoint] = variable;
                }
            }
        }
    }

    public GRBVar getX(Commercial commercial, Inventory inventory, int timePoint) {
        return X[commercial.getId()][inventory.getId()][timePoint];
    }
}
