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
        // solveWithDiscreteMip("instances/density=HIGH_nInventory=10_nHours=3_seed=1.json");
        var problem =
                new JsonParser()
                        .readData("instances/density=HIGH_nInventory=15_nHours=4_seed=1.json");

        var graspConfig =
                new GraspSettings(
                        SearchMode.FIRST_IMPROVEMENT,
                        300,
                        new LocalSearchSettings(
                                new ArrayList<>(
                                        List.of(
                                                "insert",
                                                "outOfPool",
                                                "interSwap",
                                                "shift",
                                                "transfer",
                                                "intraSwap")),
                                0.5),
                        new ConstructiveHeuristicSettings(0.5, 2),
                        new AlphaGeneratorUniform(0.1, 1),
                        0,
                        "instances/1.json");

        // var beeColonySettings = new BeeColonySettings(
        // 1,
        // 1000,
        // 0.9,
        // 10,
        // 0.99,
        // 1,
        // "");

        var grasp = new GraspWithPathRelinking(problem, graspConfig);
        //                 var parallelGrasp = new ParallelGraspWithPathRelinking(
        //                 problem,
        //                 graspConfig,
        //                 -1);

        // OrienteeringData orienteeringData = ReduceProblemToVRP.reduce(problem);
        // var beeColony = new BeeColonyAlgorithm(orienteeringData, beeColonySettings,
        // problem);
    }

    private static void solveWithDiscreteMip(String fileName) throws Exception {
        var seed = 0;
        var problem = new JsonParser().readData(fileName);
        // var parameters = new GraspSettings(
        // SearchMode.BEST_IMPROVEMENT,
        // 30,
        // new LocalSearchSettings(new ArrayList<>(
        // List.of(
        // "insert",
        // "outOfPool",
        // "interSwap",
        // "shift"
        //// "transfer"
        //// "intraSwap"
        // )
        // ), 0.25),
        // new ConstructiveHeuristicSettings(0.5, 2),
        // new AlphaGeneratorUniform(0.1, 0.5),
        // 0,
        // "instances/1.json"
        // );
        //
        // var grasp = new GraspWithPathRelinking(problem, parameters);
        // var bestSolution = grasp.getSolution().getBestSolution();

        // var model = new DiscreteTimeModel(problem);
        // model.giveWarmStart(bestSolution);
        //
        // model.getModel().optimize();
    }
}
