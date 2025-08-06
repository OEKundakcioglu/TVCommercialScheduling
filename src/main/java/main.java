import data.problemBuilders.JsonParser;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.util.ArrayList;
import java.util.List;

public class main {

    public static void main(String[] args) throws Exception {
        solveWithDiscreteMip("instances/3.json");
    }

    private static void solveWithDiscreteMip(String fileName) throws Exception {
        var seed = 0;
        var problem = new JsonParser().readData(fileName);
        var parameters = new GraspSettings(
                SearchMode.BEST_IMPROVEMENT,
                30,
                new LocalSearchSettings(new ArrayList<>(
                        List.of(
                                "insert",
                                "outOfPool",
                                "interSwap",
                                "shift"
//                                "transfer"
//                                "intraSwap"
                               )
                ), 0.25),
                new ConstructiveHeuristicSettings(0.5, 2),
                new AlphaGeneratorUniform(0.1, 0.5),
                0,
                "instances/1.json"
        );

        var grasp = new GraspWithPathRelinking(problem, parameters);
        var bestSolution = grasp.getSolution().getBestSolution();

//        var model = new DiscreteTimeModel(problem);
//        model.giveWarmStart(bestSolution);
//
//        model.getModel().optimize();
    }
}
