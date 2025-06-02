import data.Utils;
import data.problemBuilders.JsonParser;

import randomProblemGenerator.DistributionsJsonLoader;
import randomProblemGenerator.RandomProblemGenerator;

import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;

import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;

import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class main {
    public static void main(String[] args) throws Exception {
        var problemData = new JsonParser().readData("test.json");
        problemData.writeToPath(Paths.get("test.json"));

        int seed = 0;
        int nCommercial = 10;
        int nInventory = 10;
        int nHours = 10;
        double suitableInvProbability = 0.5;
        var randomGeneratorConf =
                new DistributionsJsonLoader(Paths.get("distributions.json"), seed)
                        .load(nInventory, nHours, 0.9);

        problemData = new RandomProblemGenerator(randomGeneratorConf).generate();

        var graspParameters =
                new GraspSettings(
                        SearchMode.BEST_IMPROVEMENT,
                        100,
                        new LocalSearchSettings(
                                List.of(
                                        "outOfPool",
                                        "interSwap",
                                        "insert",
                                        "transfer",
                                        "intraSwap",
                                        "shift"),
                                0.5),
                        new ConstructiveHeuristicSettings(0.5, 2),
                        new Random(0),
                        new AlphaGeneratorConstant(0.5),
                        1,
                        "");

        var solution = new GraspWithPathRelinking(problemData, graspParameters).getSolution();

        Utils.feasibilityCheck(solution.getBestSolution());
    }

}
