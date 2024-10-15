package discreteTimeModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import data.Commercial;
import data.Inventory;
import model.ProblemParameters;

import java.util.List;
import java.util.Map;

public class DiscreteTimeObjective {

    public static void setObjective(GRBModel model,
                                    DiscreteTimeVariables variables,
                                    ProblemParameters problemParameters,
                                    Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints) throws GRBException {
        var obj = new GRBLinExpr();
        for (var inventory : problemParameters.getSetOfInventories()) {
            for (var commercial : inventory.getSetOfSuitableCommercials()) {
                for (var t : feasibleTimePoints.get(commercial).get(inventory)) {
                    var revenue = commercial.getRevenue(inventory, t);
                    var variable = variables.getX(commercial, inventory, t);
                    obj.addTerm(revenue, variable);
                }
            }
        }

        model.setObjective(obj, GRB.MAXIMIZE);
    }
}
