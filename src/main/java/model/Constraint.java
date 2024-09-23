package model;

import com.gurobi.gurobi.*;
import data.Commercial;
import data.Inventory;
import data.enums.ATTENTION;

import java.util.List;

public class Constraint {

    public static void setConstraints(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        setEachCommercialAtMostOnce(parameters, model, variables);
        setAtMostOneCommercialAtEachSpot(parameters, model, variables);
        setOrdering(parameters, model, variables);
        setInventoryDuration(parameters, model, variables);
        setHourlyDurationLimit(parameters, model, variables);
        setUnusedS0(parameters, model, variables);
        setSOLink(parameters, model, variables);
        setSZLink(parameters, model, variables);
        setZ1Link(parameters, model, variables);
        setMustBeBroadcastedFirst(parameters, model, variables);
        setMustBeBroadcastedLast(parameters, model, variables);
        setF30(parameters, model, variables);
        setF60(parameters, model, variables);
        setSubsequentCommercialConstraint(parameters, model, variables);
    }

    private static void setEachCommercialAtMostOnce(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfCommercials()) {
            GRBLinExpr lhs = new GRBLinExpr();

            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                    GRBVar o = variables.getO().get(commercial).get(inventory).get(n);
                    lhs.addTerm(1, o);
                }
            }


            model.addConstr(lhs, GRB.LESS_EQUAL, 1, String.format("each_commercial_at_most_once[%d]", commercial.getId()));
        }
    }

    private static void setAtMostOneCommercialAtEachSpot(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Inventory inventory : parameters.getSetOfInventories()) {
            for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                    GRBVar o = variables.getO().get(commercial).get(inventory).get(n);
                    expr.addTerm(1, o);
                }
                model.addConstr(expr, GRB.LESS_EQUAL, 1, String.format("one_commercial_at_each_spot[%d][%d]", inventory.getId(), n));
            }
        }
    }

    private static void setOrdering(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Inventory inventory : parameters.getSetOfInventories()) {
            for (int n = 0; n < inventory.getMaxCommercialCount() - 1; n++) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                    GRBVar o = variables.getO().get(commercial).get(inventory).get(n);
                    lhs.addTerm(1, o);
                }

                GRBLinExpr rhs = new GRBLinExpr();
                for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                    GRBVar o = variables.getO().get(commercial).get(inventory).get(n + 1);
                    rhs.addTerm(1, o);
                }

                model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, String.format("ordering[%d][%d]", inventory.getId(), n));
            }
        }
    }

    private static void setInventoryDuration(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Inventory inventory : parameters.getSetOfInventories()) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                    GRBVar o = variables.getO().get(commercial).get(inventory).get(n);
                    lhs.addTerm(commercial.getDuration(), o);
                }
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, inventory.getDuration(), String.format("inventory_capacity[%d]", inventory.getId()));
        }
    }

    private static void setHourlyDurationLimit(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (var hour : parameters.getSetOfHours()) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (Inventory inventory : parameters.getSetOfInventories().stream().filter(inventory -> inventory.getHour() == hour).toList()) {
                for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                    for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                        GRBVar o = variables.getO().get(commercial).get(inventory).get(n);
                        lhs.addTerm(commercial.getDuration(), o);
                    }
                }
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, 720, String.format("hourly_duration_limit[%d]", hour));
        }
    }

    private static void setUnusedS0(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfCommercials()) {
            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    lhs.addTerm(1, variables.getS().get(commercial).get(inventory).get(n));

                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addTerm(inventory.getDuration(), variables.getO().get(commercial).get(inventory).get(n));

                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("unused_S_0[%d][%d][%d]", commercial.getId(), inventory.getId(), n));
                }
            }
        }
    }

    private static void setSOLink(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Inventory inventory : parameters.getSetOfInventories()) {
            for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                    GRBLinExpr lhsUpperBound = new GRBLinExpr();
                    lhsUpperBound.addTerm(1, variables.getS().get(commercial).get(inventory).get(n));

                    GRBLinExpr rhsUpperBound = new GRBLinExpr();
                    rhsUpperBound.addConstant(0);
                    for (Commercial j : inventory.getSetOfSuitableCommercials()) {
                        for (int m = 0; m < n; m++) {
                            rhsUpperBound.addTerm(j.getDuration(),
                                    variables.getO().get(j).get(inventory).get(m));
                        }
                    }
                    model.addConstr(lhsUpperBound, GRB.LESS_EQUAL, rhsUpperBound, String.format("start_time_upper_bound[%d][%d][%d]", commercial.getId(), inventory.getId(), n));

                    GRBLinExpr lhsLowerBound = new GRBLinExpr();
                    lhsLowerBound.addTerm(1, variables.getS().get(commercial).get(inventory).get(n));

                    GRBLinExpr rhsLowerBound = new GRBLinExpr();
                    rhsLowerBound.addConstant(0);
                    for (Commercial j : inventory.getSetOfSuitableCommercials()) {
                        for (int m = 0; m < n; m++) {
                            rhsLowerBound.addTerm(j.getDuration(),
                                    variables.getO().get(j).get(inventory).get(m));

                            rhsLowerBound.addTerm(inventory.getDuration(),
                                    variables.getO().get(commercial).get(inventory).get(n));
                            rhsLowerBound.addConstant(-inventory.getDuration());
                        }
                    }
                    model.addConstr(lhsLowerBound, GRB.GREATER_EQUAL, rhsLowerBound, String.format("start_time_lower_bound[%d][%d][%d]", commercial.getId(), inventory.getId(), n));
                }
            }
        }
    }

    private static void setSZLink(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Inventory inventory : parameters.getSetOfInventories()) {
            for (Commercial commercial : inventory.getSetOfSuitableCommercials()) {
                for (int t = 1; t <= inventory.getDurationInMinutes(); t++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                        lhs.addTerm(1,
                                variables.getS().get(commercial).get(inventory).get(n));
                    }

                    GRBLinExpr rhsUpperBound = new GRBLinExpr();
                    rhsUpperBound.addConstant(60 * t);
                    rhsUpperBound.addConstant(inventory.getDuration());
                    rhsUpperBound.addTerm(-inventory.getDuration(),
                            variables.getZ().get(commercial).get(inventory).get(t));
                    rhsUpperBound.addConstant(-0.5);

                    model.addConstr(lhs, GRB.LESS_EQUAL, rhsUpperBound, String.format("s_z_link_upper_bound[%d][%d][%d]", inventory.getId(), commercial.getId(), t));

                    GRBLinExpr rhsLowerBound = new GRBLinExpr();
                    rhsLowerBound.addTerm(60 * (t - 1),
                            variables.getZ().get(commercial).get(inventory).get(t));

                    model.addConstr(lhs, GRB.GREATER_EQUAL, rhsLowerBound, String.format("s_z_link_lower_bound[%d][%d][%d]", inventory.getId(), commercial.getId(), t));
                }
            }
        }
    }

    private static void setZ1Link(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfCommercials()) {
            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                int t = 1;

                GRBLinExpr lhs = new GRBLinExpr();
                for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                    lhs.addTerm(1, variables.getO().get(commercial).get(inventory).get(n));
                }

                model.addConstr(lhs, GRB.GREATER_EQUAL, variables.getZ().get(commercial).get(inventory).get(t), String.format("first_minute_link_[%d][%d]", commercial.getId(), inventory.getId()));
            }
        }
    }

    private static void setMustBeBroadcastedFirst(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfFirstCommercials()) {
            for (Inventory inventory : commercial.getSetOfSuitableInvByAttention().get(ATTENTION.FIRST)) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int n = 1; n < inventory.getMaxCommercialCount(); n++) {
                    lhs.addTerm(1, variables.getO().get(commercial).get(inventory).get(n));
                }

                model.addConstr(lhs, GRB.EQUAL, 0, String.format("must_be_broad_casted_first_[%d][%d]", commercial.getId(), inventory.getId()));
            }
        }
    }

    private static void setMustBeBroadcastedLast(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfLastCommercials()) {
            for (Inventory inventory : commercial.getSetOfSuitableInvByAttention().get(ATTENTION.LAST)) {
                for (int n = 0; n < inventory.getMaxCommercialCount() - 1; n++) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    lhs.addTerm(1, variables.getO().get(commercial).get(inventory).get(n));

                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(1);
                    for (Commercial comm : inventory.getSetOfSuitableCommercials()) {
                        rhs.addTerm(-1, variables.getO().get(comm).get(inventory).get(n + 1));
                    }

                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("must_be_broad_casted_last_[%d][%d][%d]", commercial.getId(), inventory.getId(), n));
                }
            }
        }
    }

    private static void setSubsequentCommercialConstraint(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        List<Integer> groups = parameters.getSetOfCommercials().stream().map(Commercial::getGroup).distinct().toList();

        for (var group : groups) {
            List<Commercial> commercials = parameters.getSetOfCommercials().stream().filter(c -> c.getGroup() == group).toList();
            for (int firstCommIndex = 0; firstCommIndex < commercials.size() - 1; firstCommIndex++) {
                for (int secondCommIndex = firstCommIndex + 1; secondCommIndex < commercials.size(); secondCommIndex++) {
                    Commercial firstCommercial = commercials.get(firstCommIndex);
                    Commercial secondCommercial = commercials.get(secondCommIndex);

                    for (Inventory inventory : firstCommercial.getSetOfSuitableInv()) {
                        if (!secondCommercial.getSetOfSuitableInv().contains(inventory)) continue;
                        for (int n = 0; n < inventory.getMaxCommercialCount() - 1; n++) {
                            GRBLinExpr lhs = new GRBLinExpr();

                            lhs.addTerm(1, variables.getO().get(firstCommercial).get(inventory).get(n));
                            lhs.addTerm(1, variables.getO().get(secondCommercial).get(inventory).get(n + 1));

                            model.addConstr(lhs, GRB.LESS_EQUAL, 1, String.format("subsequent_commercial_constraint_1_[%d][%d][%d][%d]", firstCommercial.getId(), secondCommercial.getId(), inventory.getId(), n));

                            lhs = new GRBLinExpr();
                            lhs.addTerm(1, variables.getO().get(secondCommercial).get(inventory).get(n));
                            lhs.addTerm(1, variables.getO().get(firstCommercial).get(inventory).get(n + 1));

                            model.addConstr(lhs, GRB.LESS_EQUAL, 1, String.format("subsequent_commercial_constraint_2_[%d][%d][%d][%d]", firstCommercial.getId(), secondCommercial.getId(), inventory.getId(), n));
                        }
                    }
                }
            }
        }
    }

    private static void setF30(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfF30Commercials()){
            GRBLinExpr lhs = new GRBLinExpr();
            for (Inventory inventory : commercial.getSetOfSuitableInvByAttention().get(ATTENTION.F30)){
                for (int n = 0; n<inventory.getMaxCommercialCount(); n++){
                    lhs.addTerm(1, variables.getS().get(commercial).get(inventory).get(n));
                }
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, 30, String.format("f30_constraint_[%d]", commercial.getId()));
        }
    }

    private static void setF60(ProblemParameters parameters, GRBModel model, Variables variables) throws GRBException {
        for (Commercial commercial : parameters.getSetOfF60Commercials()){
            GRBLinExpr lhs = new GRBLinExpr();
            for (Inventory inventory : commercial.getSetOfSuitableInvByAttention().get(ATTENTION.F60)){
                for (int n = 0; n<inventory.getMaxCommercialCount(); n++){
                    lhs.addTerm(1, variables.getS().get(commercial).get(inventory).get(n));
                }
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, 60, String.format("f60_constraint_[%d]", commercial.getId()));
        }
    }
}
