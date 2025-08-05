import data.problemBuilders.JsonParser;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class main {

    public static void main(String[] args) throws Exception {
        solveWithDiscreteMip("instances/1.json");
    }

    private static void solveWithDiscreteMip(String fileName) throws Exception {
        var problem = new JsonParser().readData(fileName);
        var parameters = new GraspSettings(
                SearchMode.BEST_IMPROVEMENT,
                50,
                new LocalSearchSettings(new ArrayList<>(
                        List.of(
                                "outOfPool",
                                "interSwap",
                                "insert",
                                "transfer",
                                "intraSwap",
                                "shift"
                               )
                ), 0.5),
                new ConstructiveHeuristicSettings(0.5, 2),
                new Random(0),
                new AlphaGeneratorUniform(new Random(0), 0.1, 1),
                0,
                "instances/1.json"
        );

        var grasp = new GraspWithPathRelinking(problem, parameters);
        var bestSolution = grasp.getSolution().getBestSolution();

        var model = new DiscreteTimeModel(problem);
        model.giveWarmStart(bestSolution);

        model.getModel().optimize();
    }
}
