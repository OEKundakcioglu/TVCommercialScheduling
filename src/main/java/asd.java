import com.google.common.collect.Collections2;

import data.ProblemParameters;
import data.Solution;

import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;

import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class asd {
    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        var parameters = new ProblemParameters();
        parameters.readData("instances/1.json");

        //        var orienteeringData = ReduceProblemToVRP.reduce(parameters);
        //        var algorithm =
        //                new BeeColonyAlgorithm(
        //                        orienteeringData,
        //                        new BeeColonySettings(30, 6000, 0.9, 90, 0.98),
        //                        parameters);

        //        var model =
        //                new ModelSolver(
        //                        new DiscreteTimeModel(parameters),
        //                        parameters,
        //                        new MipRunSettings(List.of(90), ""));
        //        Utils.feasibilityCheck(model.getSolution().getBestSolution());

        var permutes =
                List.copyOf(
                        Collections2.permutations(
                                List.of(
                                        "shift",
                                        "intraSwap",
                                        "outOfPool",
                                        "transfer",
                                        "interSwap",
                                        "insert")));

        var sols = new ArrayList<List<Object>>();

        for (var permute : permutes) {
            var random = new Random(0);
            var grasp =
                    new GraspWithPathRelinking(
                            parameters,
                            new GraspSettings(
                                    true,
                                    120,
                                    new LocalSearchSettings(permute),
                                    new ConstructiveHeuristicSettings(0.2, 5),
                                    random,
                                    new AlphaGeneratorUniform(random, 0.1, 0.5),
                                    1,
                                    "instances/1.json"));

            sols.add(List.of(permute, grasp.getSolution().getBestSolution().revenue));
        }

        sols.sort(Comparator.comparing(i -> (double) i.get(1)));

        BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
        for (var sol : sols) {
            List<String> permute = (List<String>) sol.getFirst();
            writer.write(Arrays.toString(permute.toArray()));
            System.out.println(Arrays.toString(permute.toArray()));

            double revenue = (double) sol.get(1);
            writer.write("\t " + (int) revenue + System.lineSeparator());
            System.out.println((int) revenue);
        }
        writer.close();
    }
}
