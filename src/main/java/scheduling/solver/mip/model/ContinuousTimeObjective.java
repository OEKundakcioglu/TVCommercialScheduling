package scheduling.solver.mip.model;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;

class ContinuousTimeObjective {

    private static final int SEC_IN_MIN = 60;

    private final GRBModel model;
    private final Problem problem;
    private final ContinuousTimeVariables variables;

    ContinuousTimeObjective(GRBModel model, Problem problem, ContinuousTimeVariables variables) {
        this.model = model;
        this.problem = problem;
        this.variables = variables;
    }

    void setObjective() throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();

        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                int numWindows = inv.getDurationInMinutes();
                for (int w = 1; w <= numWindows; w++) {
                    double revenue = problem.getRevenue(c, invId, (w - 1) * SEC_IN_MIN);
                    expr.addTerm(revenue, variables.getZ(c, invId, w));
                }
            }
        }

        model.setObjective(expr, GRB.MAXIMIZE);
    }
}
