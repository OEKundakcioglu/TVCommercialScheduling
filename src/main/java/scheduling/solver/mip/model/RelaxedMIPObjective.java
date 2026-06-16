package scheduling.solver.mip.model;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.solver.mip.RelaxedMIPReturnMode;

class RelaxedMIPObjective {

    private static final int SEC_IN_MIN = 60;

    private final GRBModel model;
    private final Problem problem;
    private final RelaxedMIPVariables variables;
    private final RelaxedMIPReturnMode returnMode;

    RelaxedMIPObjective(
            GRBModel model,
            Problem problem,
            RelaxedMIPVariables variables,
            RelaxedMIPReturnMode returnMode) {
        this.model = model;
        this.problem = problem;
        this.variables = variables;
        this.returnMode = returnMode;
    }

    void setObjective() throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();

        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                double revenue = revenueInBreak(problem, c, invId, returnMode);
                for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                    expr.addTerm(revenue, variables.getO(c, invId, n));
                }
            }
        }

        model.setObjective(expr, GRB.MAXIMIZE);
    }

    static double bestRevenueInBreak(Problem problem, int commId, int invId) {
        return revenueInBreak(problem, commId, invId, RelaxedMIPReturnMode.BEST);
    }

    static double averageRevenueInBreak(Problem problem, int commId, int invId) {
        return revenueInBreak(problem, commId, invId, RelaxedMIPReturnMode.AVERAGE);
    }

    static double worstRevenueInBreak(Problem problem, int commId, int invId) {
        return revenueInBreak(problem, commId, invId, RelaxedMIPReturnMode.WORST);
    }

    private static double revenueInBreak(
            Problem problem, int commId, int invId, RelaxedMIPReturnMode returnMode) {
        Inventory inv = problem.getInventory(invId);
        int windows = inv.getDurationInMinutes();
        double bestRevenue = problem.getRevenue(commId, invId, 0);
        double worstRevenue = bestRevenue;
        double totalRevenue = 0.0;

        for (int w = 1; w <= windows; w++) {
            double revenue = problem.getRevenue(commId, invId, (w - 1) * SEC_IN_MIN);
            bestRevenue = Math.max(bestRevenue, revenue);
            worstRevenue = Math.min(worstRevenue, revenue);
            totalRevenue += revenue;
        }

        return switch (returnMode) {
            case BEST -> bestRevenue;
            case AVERAGE -> totalRevenue / windows;
            case WORST -> worstRevenue;
        };
    }
}
