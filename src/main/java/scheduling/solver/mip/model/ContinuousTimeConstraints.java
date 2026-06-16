package scheduling.solver.mip.model;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;

class ContinuousTimeConstraints {

    private static final double EPSILON = 1;
    private static final int SEC_IN_MIN = 60;

    private final GRBModel model;
    private final Problem problem;
    private final ContinuousTimeVariables variables;

    ContinuousTimeConstraints(GRBModel model, Problem problem, ContinuousTimeVariables variables) {
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
        addStartTimeLinkage();
        addTimeWindowAssignment();
        addGroupNonConsecutivity();
        addFlagSatisfaction();
        addFirstTypeFlags();
        addLastTypeFlags();
    }

    // Eq. 2: Each commercial aired at most once
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

    // Eq. 3: At most 1 commercial per slot
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

    // Eq. 4: Slot n+1 occupied only if slot n is
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

    // Eq. 5: Total duration within break capacity
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

    // Eq. 6: Hourly broadcast duration limit
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

    // Eq. 7, 8, 9: Link O to continuous start time S
    private void addStartTimeLinkage() throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                int maxSlots = inv.getMaxCommercialCount();

                for (int n = 0; n < maxSlots; n++) {
                    // Eq. 7: S[c][i][n] <= duration(i) * O[c][i][n]
                    GRBLinExpr lhs7 = new GRBLinExpr();
                    lhs7.addTerm(1, variables.getS(c, invId, n));

                    GRBLinExpr rhs7 = new GRBLinExpr();
                    rhs7.addTerm(inv.getDuration(), variables.getO(c, invId, n));
                    model.addConstr(
                            lhs7, GRB.LESS_EQUAL, rhs7, "start_ub_" + c + "_" + invId + "_" + n);

                    if (n == 0) {
                        // Eq 8 for n=0: S[c][i][0] <= 0 (empty sum on RHS)
                        GRBLinExpr lhs8 = new GRBLinExpr();
                        lhs8.addTerm(1, variables.getS(c, invId, 0));
                        model.addConstr(lhs8, GRB.LESS_EQUAL, 0, "start_zero_" + c + "_" + invId);
                        continue; // Eq 9 for n=0 is redundant (S >= 0 from domain)
                    }

                    // Eq. 8: S[c][i][n] <= Σ_{c',m<n} duration(c') * O[c'][i][m]
                    GRBLinExpr lhs8 = new GRBLinExpr();
                    lhs8.addTerm(1, variables.getS(c, invId, n));

                    GRBLinExpr rhs8 = new GRBLinExpr();
                    for (int commPrimeId : problem.getSuitableCommercials(invId)) {
                        int commDuration = problem.getCommercial(commPrimeId).getDuration();
                        for (int m = 0; m < n; m++) {
                            rhs8.addTerm(commDuration, variables.getO(commPrimeId, invId, m));
                        }
                    }
                    model.addConstr(
                            lhs8,
                            GRB.LESS_EQUAL,
                            rhs8,
                            "start_link_ub_" + c + "_" + invId + "_" + n);

                    // Eq. 9: S[c][i][n] >= Σ_{c',m<n} duration(c') * O[c'][i][m] - duration(i) * (1
                    // - O[c][i][n])
                    GRBLinExpr lhs9 = new GRBLinExpr();
                    lhs9.addTerm(1, variables.getS(c, invId, n));

                    GRBLinExpr rhs9 = new GRBLinExpr();
                    for (int commPrimeId : problem.getSuitableCommercials(invId)) {
                        int commDuration = problem.getCommercial(commPrimeId).getDuration();
                        for (int m = 0; m < n; m++) {
                            rhs9.addTerm(commDuration, variables.getO(commPrimeId, invId, m));
                        }
                    }
                    rhs9.addConstant(-inv.getDuration());
                    rhs9.addTerm(inv.getDuration(), variables.getO(c, invId, n));
                    model.addConstr(
                            lhs9,
                            GRB.GREATER_EQUAL,
                            rhs9,
                            "start_link_lb_" + c + "_" + invId + "_" + n);
                }
            }
        }
    }

    // Eq. 10, 11, 12: Link S to time window indicator Z
    private void addTimeWindowAssignment() throws GRBException {
        for (Commercial comm : problem.getCommercials()) {
            int c = comm.getId();
            for (int invId : problem.getSuitableInventories(c)) {
                Inventory inv = problem.getInventory(invId);
                int maxSlots = inv.getMaxCommercialCount();
                int numWindows = inv.getDurationInMinutes();

                for (int w = 1; w <= numWindows; w++) {
                    String suffix = c + "_" + invId + "_" + w;

                    // Eq. 10: sum_n S <= SEC_IN_MIN * w + duration(i) * (1 - Z) - EPSILON
                    GRBLinExpr lhs10 = new GRBLinExpr();
                    for (int n = 0; n < maxSlots; n++) {
                        lhs10.addTerm(1, variables.getS(c, invId, n));
                    }

                    GRBLinExpr rhs10 = new GRBLinExpr();
                    rhs10.addConstant(SEC_IN_MIN * w + inv.getDuration() - EPSILON);
                    rhs10.addTerm(-inv.getDuration(), variables.getZ(c, invId, w));
                    model.addConstr(lhs10, GRB.LESS_EQUAL, rhs10, "tw_ub_" + suffix);

                    // Eq. 11: sum_n S >= SEC_IN_MIN * (w - 1) * Z
                    GRBLinExpr lhs11 = new GRBLinExpr();
                    for (int n = 0; n < maxSlots; n++) {
                        lhs11.addTerm(1, variables.getS(c, invId, n));
                    }

                    GRBLinExpr rhs11 = new GRBLinExpr();
                    rhs11.addTerm(SEC_IN_MIN * (w - 1), variables.getZ(c, invId, w));
                    model.addConstr(lhs11, GRB.GREATER_EQUAL, rhs11, "tw_lb_" + suffix);

                    // Eq. 12: sum_n O >= Z
                    GRBLinExpr lhs12 = new GRBLinExpr();
                    for (int n = 0; n < maxSlots; n++) {
                        lhs12.addTerm(1, variables.getO(c, invId, n));
                    }

                    GRBLinExpr rhs12 = new GRBLinExpr();
                    rhs12.addTerm(1, variables.getZ(c, invId, w));
                    model.addConstr(lhs12, GRB.GREATER_EQUAL, rhs12, "tw_link_" + suffix);
                }
            }
        }
    }

    // Eq. 13, 14: Same-group commercials not consecutive
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
                        // Eq. 13: O[c1][i][n] + O[c2][i][n+1] <= 1
                        GRBLinExpr expr13 = new GRBLinExpr();
                        expr13.addTerm(1, variables.getO(c1, i, n));
                        expr13.addTerm(1, variables.getO(c2, i, n + 1));
                        model.addConstr(
                                expr13,
                                GRB.LESS_EQUAL,
                                1,
                                "no_consec_" + c1 + "_" + c2 + "_" + i + "_" + n);

                        // Eq. 14: O[c2][i][n] + O[c1][i][n+1] <= 1
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

    // Eq. 15: At least one flag satisfied if scheduled
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

    // Eq. 16: First-type flag position enforcement
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
                        // O[c][i][n] <= 1 - y[c][i][f]
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

    // Eq. 17, 18: Last-type flag position enforcement
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

                        // Eq. 17: A_n^i <= (maxP-1) + bigM*(2 - O[c][i][n] - y[c][i][f])
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

                        // Eq. 18: A_n^i >= (minP-1) - bigM*(2 - O[c][i][n] - y[c][i][f])
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
