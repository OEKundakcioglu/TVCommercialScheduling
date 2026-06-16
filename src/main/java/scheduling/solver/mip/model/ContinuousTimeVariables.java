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

class ContinuousTimeVariables {

    private final GRBVar[][][] orderingVars;
    private final GRBVar[][][] timeWindowVars;
    private final GRBVar[][][] startTimeVars;
    private final GRBVar[][][] flagVars;

    ContinuousTimeVariables(GRBModel model, Problem problem) throws GRBException {
        int numComms = problem.getCommercials().length;
        int numInvs = problem.getInventories().length;

        orderingVars = new GRBVar[numComms][numInvs][];
        timeWindowVars = new GRBVar[numComms][numInvs][];
        startTimeVars = new GRBVar[numComms][numInvs][];
        flagVars = new GRBVar[numComms][numInvs][];

        createVariables(model, problem);
    }

    GRBVar getO(int commId, int invId, int slot) {
        return checkNotNull(orderingVars[commId][invId][slot]);
    }

    GRBVar getZ(int commId, int invId, int timeWindow) {
        return checkNotNull(timeWindowVars[commId][invId][timeWindow - 1]);
    }

    GRBVar getS(int commId, int invId, int slot) {
        return checkNotNull(startTimeVars[commId][invId][slot]);
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
                int numWindows = inv.getDurationInMinutes();

                createOrderingVars(model, c, invId, maxSlots);
                createTimeWindowVars(model, c, invId, numWindows);
                createStartTimeVars(model, c, invId, maxSlots, inv.getDuration());
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

    private void createTimeWindowVars(GRBModel model, int c, int i, int numWindows)
            throws GRBException {
        timeWindowVars[c][i] = new GRBVar[numWindows];
        for (int w = 0; w < numWindows; w++) {
            timeWindowVars[c][i][w] =
                    model.addVar(0, 1, 0, GRB.BINARY, "Z_" + c + "_" + i + "_" + (w + 1));
        }
    }

    private void createStartTimeVars(GRBModel model, int c, int i, int maxSlots, int invDuration)
            throws GRBException {
        startTimeVars[c][i] = new GRBVar[maxSlots];
        for (int n = 0; n < maxSlots; n++) {
            startTimeVars[c][i][n] =
                    model.addVar(0, invDuration, 0, GRB.CONTINUOUS, "S_" + c + "_" + i + "_" + n);
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
