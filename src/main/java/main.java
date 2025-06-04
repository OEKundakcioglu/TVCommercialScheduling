import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;

import randomProblemGenerator.DistributionsJsonLoader;
import randomProblemGenerator.RandomProblemGenerator;

import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;

import runParameters.MipRunSettings;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class main {
//    private static final MipRunSettings mipRunSettings = new MipRunSettings()
//
//    public static void main(String[] args) throws Exception {
//        var folder = new File("instances");
//        var files = folder.listFiles((dir, name) -> name.endsWith(".json"));
//    }
//
//    private static void solveWithDiscreteMip(String fileName) throws Exception {
//
//
//        var problem = new JsonParser().readData(fileName);
//        var model = new DiscreteTimeModel(problem);
//        var solver = new ModelSolver(model, problem, );
//    }
}
