package solvers.mipSolvers.discreteTimeModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import data.*;
import data.enums.ATTENTION;
import solvers.mipSolvers.BaseModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@SuppressWarnings("FieldCanBeLocal")
public class DiscreteTimeModel extends BaseModel {
    private final ProblemParameters parameters;
    private final Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints;
    private final Map<Commercial, Map<Integer, List<Integer>>> timeCoverages;
    private DiscreteTimeVariables variables;

    public DiscreteTimeModel(ProblemParameters parameters) throws GRBException {
        super();
        this.parameters = parameters;
        this.feasibleTimePoints = computeFeasibleTimePoints();
        this.timeCoverages = computeTimeCoverages();
    }

    @Override
    public GRBModel getModel() {
        return model;
    }

    public void build() throws GRBException {
        this.variables = new DiscreteTimeVariables(feasibleTimePoints, parameters, model);
        DiscreteTimeObjective.setObjective(model, variables, parameters, feasibleTimePoints);
        DiscreteTimeConstraints.setConstraints(
                parameters, model, variables, feasibleTimePoints, timeCoverages);
    }

    private Map<Commercial, Map<Inventory, List<Integer>>> computeFeasibleTimePoints() {
        var map = new HashMap<Commercial, Map<Inventory, List<Integer>>>();
        for (var commercial : parameters.getSetOfCommercials()) {
            var commercialFeasibleTimePoints = new HashMap<Inventory, List<Integer>>();
            for (var inventory : commercial.getSetOfSuitableInv()) {
                var timePoints = computeFeasibleTimePoints(commercial, inventory);
                commercialFeasibleTimePoints.put(inventory, timePoints);
            }
            map.put(commercial, commercialFeasibleTimePoints);
        }

        return map;
    }

    private List<Integer> computeFeasibleTimePoints(Commercial commercial, Inventory inventory) {
        var attention = commercial.getAttentionMap().get(inventory);

        if (attention == ATTENTION.NONE) {
            return IntStream.range(0, inventory.getDuration() - commercial.getDuration())
                    .boxed()
                    .toList();
        } else if (attention == ATTENTION.FIRST) {
            return List.of(0);
        } else if (attention == ATTENTION.F30) {
            return IntStream.range(0, 31).boxed().toList();
        } else if (attention == ATTENTION.F60) {
            return IntStream.range(0, 61).boxed().toList();
        } else if (attention == ATTENTION.LAST) {
            return IntStream.range(0, inventory.getDuration() - commercial.getDuration() + 1)
                    .boxed()
                    .toList();
        }

        throw new IllegalArgumentException("Attention type not recognized");
    }

    private Map<Commercial, Map<Integer, List<Integer>>> computeTimeCoverages() {
        int maxT =
                parameters.getSetOfInventories().stream()
                        .mapToInt(Inventory::getDuration)
                        .max()
                        .orElseThrow();

        var map = new HashMap<Commercial, Map<Integer, List<Integer>>>();
        for (var commercial : parameters.getSetOfCommercials()) {
            var commercialTimeCoverages = new HashMap<Integer, List<Integer>>();
            for (int t = 0; t <= maxT; t++) {
                var timeCoverages = computeTimeCoverages(commercial, t);
                commercialTimeCoverages.put(t, timeCoverages);
            }
            map.put(commercial, commercialTimeCoverages);
        }

        return map;
    }

    private List<Integer> computeTimeCoverages(Commercial commercial, int timePoint) {
        int lowerBound = Math.max(timePoint - commercial.getDuration(), 0);

        return IntStream.range(lowerBound, timePoint).boxed().toList();
    }

    public Solution generateSolution() throws GRBException {
        if (model.get(GRB.IntAttr.SolCount) == 0) return new Solution(List.of());

        var solutionDataList = new ArrayList<List<SolutionData>>();
        for (var ignored : parameters.getSetOfInventories()){
            solutionDataList.add(new ArrayList<>());
        }

        var maxInvDur =
                parameters.getSetOfInventories().stream()
                        .mapToInt(Inventory::getDuration)
                        .max()
                        .orElseThrow();
        for (var t = 0; t < maxInvDur + 10; t++) {
            for (var inventory : parameters.getSetOfInventories()) {
                for (var commercial : inventory.getSetOfSuitableCommercials()) {
                    var variable = variables.getX(commercial, inventory, t);
                    if (variable == null) continue;

                    if (variable.get(GRB.DoubleAttr.X) > 0.5) {
                        var solutionData = new SolutionData(commercial, inventory);
                        solutionDataList.get(inventory.getId()).add(solutionData);

                        solutionData.update(commercial.getRevenue(inventory, t), t, 0);
                    }
                }
            }
        }

        return new Solution(solutionDataList);
    }

    public void giveWarmStart(Solution solution) throws GRBException {
        for (var solutionData : solution.getSortedSolutionData()) {
            var commercial = solutionData.getCommercial();
            var inventory = solutionData.getInventory();
            var startTime = solutionData.getStartTime();

            var x = variables.getX(commercial, inventory, startTime);
            x.set(GRB.DoubleAttr.Start, 1.0);
        }
    }
}
