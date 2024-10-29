package solvers.mipSolvers.continuousTimeModel;

import com.gurobi.gurobi.*;
import data.Commercial;
import data.Inventory;
import data.ProblemParameters;

public class ContinuousTimeObjective {

    public static void setObjective(ProblemParameters parameters, GRBModel model, ContinuousTimeVariables variables) throws GRBException {
        GRBLinExpr obj = new GRBLinExpr();

        for (Commercial commercial : parameters.getSetOfCommercials()) {
            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                for (int t = 1; t <= inventory.getDurationInMinutes(); t++) {
                    double rating = parameters.getRatings().get(inventory).get(t).get(commercial.getAudienceType());
                    double revenue = commercial.getRevenue(rating);
                    GRBVar z = variables.getZ().get(commercial).get(inventory).get(t);
                    obj.addTerm(revenue, z);
                }
            }
        }

        model.setObjective(obj, GRB.MAXIMIZE);
    }
}
