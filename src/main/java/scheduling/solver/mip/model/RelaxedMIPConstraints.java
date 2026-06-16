package scheduling.solver.mip.model;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;

class RelaxedMIPConstraints {

    private final GRBModel model;
    private final Problem problem;
    private final RelaxedMIPVariables variables;

    RelaxedMIPConstraints(GRBModel model, Problem problem, RelaxedMIPVariables variables) {
        this.model = model;
        this.problem = problem;
        this.variables = variables;
    }

    void setConstraints() throws GRBException {
        addCommercialUniqueness();
        addSlotExclusivity();
        addSequentialSlotFilling();
        addBreakCapacity();
        addHourlyLimit();
        addGroupNonConsecutivity();
        addFlagSatisfaction();
        addFirstTypeFlags();
        addLastTypeFlags();
    }

    private void addCommercialUniqueness() throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            GRBLinExpr expr = new GRBLinExpr();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                    expr.addTerm(1, variables.getO(c, invId, n));
                }
            }
            model.addConstr(expr, GRB.LESS_EQUAL, 1, "uniqueness_" + c);
        }
    }

    private void addSlotExclusivity() throws GRBException {
        for (Inventory inv : problem.getInventories()) {
            int i = inv.getId();
            for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int commId : problem.getSuitableCommercials(i)) {
                    expr.addTerm(1, variables.getO(commId, i, n));
                }
                model.addConstr(expr, GRB.LESS_EQUAL, 1, "slot_excl_" + i + "_" + n);
            }
        }
    }

    private void addSequentialSlotFilling() throws GRBException {
        for (Inventory inv : problem.getInventories()) {
            int i = inv.getId();
            for (int n = 0; n < inv.getMaxCommercialCount() - 1; n++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int commId : problem.getSuitableCommercials(i)) {
                    lhs.addTerm(1, variables.getO(commId, i, n));
                }

                GRBLinExpr rhs = new GRBLinExpr();
                for (int commId : problem.getSuitableCommercials(i)) {
                    rhs.addTerm(1, variables.getO(commId, i, n + 1));
                }
                model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, "seq_fill_" + i + "_" + n);
            }
        }
    }

    private void addBreakCapacity() throws GRBException {
        for (Inventory inv : problem.getInventories()) {
            int i = inv.getId();
            GRBLinExpr expr = new GRBLinExpr();
            for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                for (int commId : problem.getSuitableCommercials(i)) {
                    int duration = problem.getCommercial(commId).getDuration();
                    expr.addTerm(duration, variables.getO(commId, i, n));
                }
            }
            model.addConstr(expr, GRB.LESS_EQUAL, inv.getDuration(), "break_cap_" + i);
        }
    }

    private void addHourlyLimit() throws GRBException {
        for (int hour : problem.getHours()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (Inventory inv : problem.getInventories()) {
                if (inv.getHour() != hour) {
                    continue;
                }
                int i = inv.getId();
                for (int commId : problem.getSuitableCommercials(i)) {
                    int duration = problem.getCommercial(commId).getDuration();
                    for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                        expr.addTerm(duration, variables.getO(commId, i, n));
                    }
                }
            }
            model.addConstr(
                    expr, GRB.LESS_EQUAL, Problem.HOURLY_BROADCAST_LIMIT, "hourly_limit_" + hour);
        }
    }

    private void addGroupNonConsecutivity() throws GRBException {
        for (Inventory inv : problem.getInventories()) {
            int i = inv.getId();
            int[] suitableComms = problem.getSuitableCommercials(i);

            for (int a = 0; a < suitableComms.length; a++) {
                int c1 = suitableComms[a];
                int g1 = problem.getCommercial(c1).getGroup();

                for (int b = a + 1; b < suitableComms.length; b++) {
                    int c2 = suitableComms[b];
                    if (problem.getCommercial(c2).getGroup() != g1) {
                        continue;
                    }

                    for (int n = 0; n < inv.getMaxCommercialCount() - 1; n++) {
                        GRBLinExpr expr13 = new GRBLinExpr();
                        expr13.addTerm(1, variables.getO(c1, i, n));
                        expr13.addTerm(1, variables.getO(c2, i, n + 1));
                        model.addConstr(
                                expr13,
                                GRB.LESS_EQUAL,
                                1,
                                "no_consec_" + c1 + "_" + c2 + "_" + i + "_" + n);

                        GRBLinExpr expr14 = new GRBLinExpr();
                        expr14.addTerm(1, variables.getO(c2, i, n));
                        expr14.addTerm(1, variables.getO(c1, i, n + 1));
                        model.addConstr(
                                expr14,
                                GRB.LESS_EQUAL,
                                1,
                                "no_consec_" + c2 + "_" + c1 + "_" + i + "_" + n);
                    }
                }
            }
        }
    }

    private void addFlagSatisfaction() throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                AttentionType[] types = problem.getAttentionTypes(c, invId);

                if (!hasNonNFlags(types)) {
                    continue;
                }

                GRBLinExpr sumY = new GRBLinExpr();
                for (int f = 0; f < types.length; f++) {
                    if (types[f] == AttentionType.N) {
                        continue;
                    }
                    sumY.addTerm(1, variables.getY(c, invId, f));
                }

                GRBLinExpr sumO = new GRBLinExpr();
                for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                    sumO.addTerm(1, variables.getO(c, invId, n));
                }

                model.addConstr(sumY, GRB.GREATER_EQUAL, sumO, "flag_sat_" + c + "_" + invId);
            }
        }
    }

    private void addFirstTypeFlags() throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                AttentionType[] types = problem.getAttentionTypes(c, invId);

                for (int f = 0; f < types.length; f++) {
                    if (!types[f].isFType()) {
                        continue;
                    }
                    int[] positions = types[f].getPositions();

                    for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                        if (isPositionInSet(n + 1, positions)) {
                            continue;
                        }
                        GRBLinExpr lhs16 = new GRBLinExpr();
                        lhs16.addTerm(1, variables.getO(c, invId, n));

                        GRBLinExpr rhs16 = new GRBLinExpr();
                        rhs16.addConstant(1);
                        rhs16.addTerm(-1, variables.getY(c, invId, f));
                        model.addConstr(
                                lhs16,
                                GRB.LESS_EQUAL,
                                rhs16,
                                "f_flag_" + c + "_" + invId + "_" + f + "_" + n);
                    }
                }
            }
        }
    }

    private void addLastTypeFlags() throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                int maxSlots = inv.getMaxCommercialCount();
                AttentionType[] types = problem.getAttentionTypes(c, invId);

                for (int f = 0; f < types.length; f++) {
                    if (!types[f].isLType()) {
                        continue;
                    }
                    int[] positions = types[f].getPositions();
                    int minP = positions[0];
                    int maxP = positions[positions.length - 1];
                    int bigM = maxSlots;

                    for (int n = 0; n < maxSlots; n++) {
                        String suffix = c + "_" + invId + "_" + f + "_" + n;

                        GRBLinExpr lhs17 = new GRBLinExpr();
                        for (int commPrime : problem.getSuitableCommercials(invId)) {
                            for (int m = n + 1; m < maxSlots; m++) {
                                lhs17.addTerm(1, variables.getO(commPrime, invId, m));
                            }
                        }

                        GRBLinExpr rhs17 = new GRBLinExpr();
                        rhs17.addConstant((maxP - 1) + 2 * bigM);
                        rhs17.addTerm(-bigM, variables.getO(c, invId, n));
                        rhs17.addTerm(-bigM, variables.getY(c, invId, f));
                        model.addConstr(lhs17, GRB.LESS_EQUAL, rhs17, "l_flag_ub_" + suffix);

                        GRBLinExpr lhs18 = new GRBLinExpr();
                        for (int commPrime : problem.getSuitableCommercials(invId)) {
                            for (int m = n + 1; m < maxSlots; m++) {
                                lhs18.addTerm(1, variables.getO(commPrime, invId, m));
                            }
                        }

                        GRBLinExpr rhs18 = new GRBLinExpr();
                        rhs18.addConstant((minP - 1) - 2 * bigM);
                        rhs18.addTerm(bigM, variables.getO(c, invId, n));
                        rhs18.addTerm(bigM, variables.getY(c, invId, f));
                        model.addConstr(lhs18, GRB.GREATER_EQUAL, rhs18, "l_flag_lb_" + suffix);
                    }
                }
            }
        }
    }

    private static boolean hasNonNFlags(AttentionType[] types) {
        for (AttentionType type : types) {
            if (type != AttentionType.N) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPositionInSet(int position, int[] positions) {
        for (int p : positions) {
            if (p == position) {
                return true;
            }
        }
        return false;
    }
}
