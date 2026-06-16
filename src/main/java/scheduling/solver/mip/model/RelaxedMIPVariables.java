package scheduling.solver.mip.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;

class RelaxedMIPVariables {

    private final GRBVar[][][] orderingVars;
    private final GRBVar[][][] flagVars;

    RelaxedMIPVariables(GRBModel model, Problem problem) throws GRBException {
        int numComms = problem.getCommercials().length;
        int numInvs = problem.getInventories().length;

        orderingVars = new GRBVar[numComms][numInvs][];
        flagVars = new GRBVar[numComms][numInvs][];

        createVariables(model, problem);
    }

    GRBVar getO(int commId, int invId, int slot) {
        return checkNotNull(orderingVars[commId][invId][slot]);
    }

    GRBVar getY(int commId, int invId, int flagIndex) {
        return checkNotNull(flagVars[commId][invId][flagIndex]);
    }

    private void createVariables(GRBModel model, Problem problem) throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                int maxSlots = inv.getMaxCommercialCount();

                createOrderingVars(model, c, invId, maxSlots);
                createFlagVars(model, problem, c, invId);
            }
        }
    }

    private void createOrderingVars(GRBModel model, int c, int i, int maxSlots)
            throws GRBException {
        orderingVars[c][i] = new GRBVar[maxSlots];
        for (int n = 0; n < maxSlots; n++) {
            orderingVars[c][i][n] = model.addVar(0, 1, 0, GRB.BINARY, "O_" + c + "_" + i + "_" + n);
        }
    }

    private void createFlagVars(GRBModel model, Problem problem, int c, int i) throws GRBException {
        AttentionType[] types = problem.getAttentionTypes(c, i);
        flagVars[c][i] = new GRBVar[types.length];
        for (int f = 0; f < types.length; f++) {
            if (types[f] == AttentionType.N) {
                continue;
            }
            flagVars[c][i][f] = model.addVar(0, 1, 0, GRB.BINARY, "y_" + c + "_" + i + "_" + f);
        }
    }
}
