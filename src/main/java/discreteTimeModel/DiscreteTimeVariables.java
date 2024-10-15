package discreteTimeModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import data.Commercial;
import data.Inventory;
import model.ProblemParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscreteTimeVariables {

    private final GRBModel model;
    private final Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> X;
    private final Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints;
    private final ProblemParameters parameters;

    public DiscreteTimeVariables(Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints, ProblemParameters parameters, GRBModel model) throws GRBException {
        this.X = new HashMap<>();
        this.feasibleTimePoints = feasibleTimePoints;
        this.parameters = parameters;
        this.model = model;

        populateX();
    }

    private void populateX() throws GRBException {
        for (var commercial : parameters.getSetOfCommercials()) {
            var commercialX = new HashMap<Inventory, Map<Integer, GRBVar>>();
            for (var inventory : commercial.getSetOfSuitableInv()) {
                var inventoryX = new HashMap<Integer, GRBVar>();
                for (var timePoint : feasibleTimePoints.get(commercial).get(inventory)) {
                    var variable = model.addVar(0, 1, 0, GRB.BINARY, "x_" + commercial.getId() + "_" + inventory.getId() + "_" + timePoint);
                    inventoryX.put(timePoint, variable);
                }
                commercialX.put(inventory, inventoryX);
            }
            X.put(commercial, commercialX);
        }
    }

    public GRBVar getX(Commercial commercial, Inventory inventory, int timePoint) {
        return X.get(commercial).get(inventory).get(timePoint);
    }
}
