package discreteTimeModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import data.*;
import data.enums.ATTENTION;
import model.ProblemParameters;

import java.util.*;
import java.util.stream.IntStream;

@SuppressWarnings("FieldCanBeLocal")
public class DiscreteTimeModel {

    private final ProblemParameters parameters;
    private final Map<Commercial, Map<Inventory, List<Integer>>> feasibleTimePoints;
    private final Map<Commercial, Map<Integer, List<Integer>>> timeCoverages;
    private final GRBEnv env;
    private final GRBModel model;
    private final DiscreteTimeVariables variables;

    public DiscreteTimeModel(ProblemParameters parameters) throws Exception {
        this.parameters = parameters;
        this.feasibleTimePoints = computeFeasibleTimePoints();
        this.timeCoverages = computeTimeCoverages();

        this.env = new GRBEnv();
        this.model = new GRBModel(env);

        model.set(GRB.DoubleParam.TimeLimit, 1.5* 3600);

        this.variables = new DiscreteTimeVariables(feasibleTimePoints, parameters, model);
        DiscreteTimeObjective.setObjective(model, variables, parameters, feasibleTimePoints);
        DiscreteTimeConstraints.setConstraints(parameters, model, variables, feasibleTimePoints, timeCoverages);

        model.write("model.lp");
        model.optimize();

        if (model.get(GRB.IntAttr.SolCount) == 0) {
            model.computeIIS();
        }

        var sol = generateSolution();


        Utils.feasibilityCheck(sol);
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
            return IntStream.range(0, inventory.getDuration() - commercial.getDuration()).boxed().toList();
        }
        else if (attention == ATTENTION.FIRST) {
            return List.of(0);
        }
        else if (attention == ATTENTION.F30) {
            return IntStream.range(0, 30).boxed().toList();
        }
        else if (attention == ATTENTION.F60) {
            return IntStream.range(0, 60).boxed().toList();
        }
        else if (attention == ATTENTION.LAST) {
            return IntStream.range(0, inventory.getDuration() - commercial.getDuration()).boxed().toList();
        }

        throw new IllegalArgumentException("Attention type not recognized");
    }

    private Map<Commercial, Map<Integer, List<Integer>>> computeTimeCoverages() {
        int maxT = parameters.getSetOfInventories().stream().mapToInt(Inventory::getDuration).max().orElseThrow();

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

    private Solution generateSolution() throws GRBException {
        var solutionDataList = new ArrayList<List<SolutionData>>();

        for (var inventory : parameters.getSetOfInventories()){
            List<SolutionData> dataList = new ArrayList<>();
            for (var commercial : inventory.getSetOfSuitableCommercials()){
                for (var t : feasibleTimePoints.get(commercial).get(inventory)){
                    var variable = variables.getX(commercial, inventory, t);
                    if (variable.get(GRB.DoubleAttr.X) > 0.5){
                        var solutionData = new SolutionData(commercial, inventory);
                        solutionData.update(
                                commercial.getRevenue(inventory, t),
                                t,
                                0
                        );
                        dataList.add(solutionData);
                    }
                }
            }

            dataList = dataList.stream().sorted(Comparator.comparing(SolutionData::getStartTime)).toList();

            solutionDataList.add(dataList);
        }

        return new Solution(solutionDataList);
    }
}
