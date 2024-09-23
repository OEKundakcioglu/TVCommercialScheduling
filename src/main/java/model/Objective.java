package model;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import data.Commercial;
import data.Inventory;

public class Objective {

    public static void setObjective(ProblemParameters parameters, GRBModel model, Variables variables) throws Exception {
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
