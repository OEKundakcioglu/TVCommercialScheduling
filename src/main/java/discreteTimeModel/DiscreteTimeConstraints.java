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

public class DiscreteTimeConstraints {

    public static void setConstraints(ProblemParameters problemParameters,
                                      GRBModel model,
                                      DiscreteTimeVariables variables,
                                      Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints,
                                      Map<Commercial, Map<Integer, List<Integer>>> timeCoverages) throws GRBException {
        DiscreteTimeConstraints.setLastAttentionConstraint(problemParameters, model, variables, feasibleTimePoints);
        DiscreteTimeConstraints.setAtMostOnceBroadcast(problemParameters, model, variables, feasibleTimePoints);
        DiscreteTimeConstraints.setCoverageConstraint(problemParameters, model, variables, feasibleTimePoints, timeCoverages);
        DiscreteTimeConstraints.setDifferentConsecutiveGroups(problemParameters, model, variables, feasibleTimePoints);
        DiscreteTimeConstraints.setHourlyDurationConstraint(problemParameters, model, variables, feasibleTimePoints);
        DiscreteTimeConstraints.setNoIdleTime(problemParameters, model, variables, feasibleTimePoints);
    }

    private static void setLastAttentionConstraint(ProblemParameters problemParameters,
                                                   GRBModel model,
                                                   DiscreteTimeVariables variables,
                                                   Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints) throws GRBException {

        for (var commercial : problemParameters.getSetOfLastCommercials()) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                for (var t : feasibleTimePoints.get(commercial).get(inventory)) {
                    int bigM = 0;
                    var lhs = new GRBLinExpr();
                    for (var cPrime : inventory.getSetOfSuitableCommercials()) {
                        if (cPrime == commercial) continue;
                        for (var tPrime : feasibleTimePoints.get(cPrime).get(inventory)) {
                            if (tPrime <= t) continue;
                            lhs.addTerm(1, variables.getX(cPrime, inventory, tPrime));
                            bigM++;
                        }
                    }

                    var rhs = new GRBLinExpr();
                    rhs.addConstant(bigM);
                    rhs.addTerm(-bigM, variables.getX(commercial, inventory, t));

                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "lastAttentionConstraint");
                }
            }
        }
    }

    private static void setAtMostOnceBroadcast(ProblemParameters problemParameters,
                                               GRBModel model,
                                               DiscreteTimeVariables variables,
                                               Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints) throws GRBException {
        for (var commercial : problemParameters.getSetOfCommercials()) {
            var lhs = new GRBLinExpr();
            for (var inventory : commercial.getSetOfSuitableInv()) {
                for (var t : feasibleTimePoints.get(commercial).get(inventory)) {
                    lhs.addTerm(1, variables.getX(commercial, inventory, t));
                }
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, 1, "atMostOnceBroadcast");
        }
    }

    private static void setCoverageConstraint(ProblemParameters problemParameters,
                                              GRBModel model,
                                              DiscreteTimeVariables variables,
                                              Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints,
                                              Map<Commercial, Map<Integer, List<Integer>>> timeCoverages) throws GRBException {
        for (var inventory : problemParameters.getSetOfInventories()) {
            for (var t = 0; t <= inventory.getDuration(); t++) {
                var lhs = new GRBLinExpr();
                for (var commercial : inventory.getSetOfSuitableCommercials()) {
                    var feasibleTimePointsCommercial = feasibleTimePoints.get(commercial).get(inventory);
                    for (var tPrime : timeCoverages.get(commercial).get(t)) {
                        if (!feasibleTimePointsCommercial.contains(tPrime)) continue;

                        var x = variables.getX(commercial, inventory, tPrime);
                        lhs.addTerm(1, x);
                    }
                }

                model.addConstr(lhs, GRB.LESS_EQUAL, 1, String.format("coverageConstraint_%s_%s", inventory.getId(), t));
            }
        }
    }

    private static void setDifferentConsecutiveGroups(
            ProblemParameters problemParameters,
            GRBModel model,
            DiscreteTimeVariables variables,
            Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints) throws GRBException {
        for (var commercial : problemParameters.getSetOfCommercials()) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                for (var t : feasibleTimePoints.get(commercial).get(inventory)) {
                    var lhs = new GRBLinExpr();
                    for (var cPrime : inventory.getSetOfSuitableCommercials()) {
                        if (cPrime == commercial) continue;
                        if (cPrime.getGroup() != commercial.getGroup()) continue;

                        var tPrime = t - cPrime.getDuration();
                        if (!feasibleTimePoints.get(cPrime).get(inventory).contains(tPrime)) continue;

                        lhs.addTerm(1, variables.getX(cPrime, inventory, t - cPrime.getDuration()));
                    }

                    var rhs = new GRBLinExpr();
                    rhs.addConstant(1);
                    rhs.addTerm(-1, variables.getX(commercial, inventory, t));

                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("differentConsecutiveGroups_%s_%s_%s", commercial.getId(), inventory.getId(), t));
                }
            }
        }
    }

    private static void setHourlyDurationConstraint(
            ProblemParameters problemParameters,
            GRBModel model,
            DiscreteTimeVariables variables,
            Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints
    ) throws GRBException {
        for (var hour : problemParameters.getSetOfHours()) {
            var lhs = new GRBLinExpr();
            for (var inventory : problemParameters.getSetOfInventories()) {
                if (inventory.getHour() != hour) continue;
                for (var commercial : inventory.getSetOfSuitableCommercials()) {
                    for (var t : feasibleTimePoints.get(commercial).get(inventory)) {
                        var x = variables.getX(commercial, inventory, t);
                        lhs.addTerm(commercial.getDuration(), x);
                    }
                }
            }

            model.addConstr(lhs, GRB.LESS_EQUAL, 720, "hourlyDurationConstraint");
        }
    }

    private static void setNoIdleTime(ProblemParameters problemParameters,
                                      GRBModel model,
                                      DiscreteTimeVariables variables,
                                      Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints) throws GRBException {
        for (var inventory : problemParameters.getSetOfInventories()) {
            for (var t = 0; t <= inventory.getDuration(); t++) {
                if (t == 0) continue;

                var lhs = new GRBLinExpr();
                for (var commercial : inventory.getSetOfSuitableCommercials()) {
                    var startTime = t - commercial.getDuration();
                    if (!feasibleTimePoints.get(commercial).get(inventory).contains(startTime)) continue;

                    var x = variables.getX(commercial, inventory, startTime);
                    lhs.addTerm(1, x);
                }

                var rhs = new GRBLinExpr();
                for (var commercial : inventory.getSetOfSuitableCommercials()) {
                    if (!feasibleTimePoints.get(commercial).get(inventory).contains(t)) continue;
                    var x = variables.getX(commercial, inventory, t);
                    rhs.addTerm(1, x);
                }

                model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, String.format("noIdleTime_%s_%s", inventory.getId(), t));
            }
        }
    }

}
