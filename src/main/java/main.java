import data.Inventory;
import data.problemBuilders.JsonParser;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class main {

    public static void main(String[] args) throws Exception {
//        solveWithDiscreteMip("instances/density=HIGH_nInventory=10_nHours=3_seed=1.json");
        var problem = new JsonParser().readData("instances/density=MEDIUM_nInventory=20_nHours=5_seed=1.json");

        var hourInvMap = new HashMap<Integer, List<Inventory>>();
        for (var hour : problem.getSetOfHours()){
            var invs = problem.getSetOfInventories().stream()
                    .filter(i -> i.getHour() == hour)
                    .toList();
            hourInvMap.put(hour, invs);
        }

        var hourToTotalInvDur = new HashMap<Integer, Integer>();
        for (var hour : problem.getSetOfHours()){
            var totalDur = hourInvMap.get(hour).stream()
                    .mapToInt(Inventory::getDuration)
                    .sum();
            hourToTotalInvDur.put(hour, totalDur);
        }

        var x=0;
    }

    private static void solveWithDiscreteMip(String fileName) throws Exception {
        var seed = 0;
        var problem = new JsonParser().readData(fileName);
//        var parameters = new GraspSettings(
//                SearchMode.BEST_IMPROVEMENT,
//                30,
//                new LocalSearchSettings(new ArrayList<>(
//                        List.of(
//                                "insert",
//                                "outOfPool",
//                                "interSwap",
//                                "shift"
////                                "transfer"
////                                "intraSwap"
//                               )
//                ), 0.25),
//                new ConstructiveHeuristicSettings(0.5, 2),
//                new AlphaGeneratorUniform(0.1, 0.5),
//                0,
//                "instances/1.json"
//        );
//
//        var grasp = new GraspWithPathRelinking(problem, parameters);
//        var bestSolution = grasp.getSolution().getBestSolution();

//        var model = new DiscreteTimeModel(problem);
//        model.giveWarmStart(bestSolution);
//
//        model.getModel().optimize();
    }
}
