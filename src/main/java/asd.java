import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import data.ProblemParameters;

import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import runParameters.PathRelinkingSettings;

import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class asd {
    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        var parameters = new ProblemParameters();
        parameters.readData("instances/5.json");

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
                        new ArrayList<>(
                                List.copyOf(
                                        Collections2.permutations(
                                                List.of(
                                                        "shift",
                                                        "intraSwap",
                                                        "outOfPool",
                                                        "transfer",
                                                        "interSwap",
                                                        "insert"))));

                var pathrelinkingCoeefOptions =
                        new ArrayList<>(List.of(
                                new PathRelinkingSettings(1)));

                var constructiveHeuristicSettings =
                        new ArrayList<>(List.of(
                                new ConstructiveHeuristicSettings(1, 5)));

                Collections.shuffle(permutes);
                Collections.shuffle(pathrelinkingCoeefOptions);
                Collections.shuffle(constructiveHeuristicSettings);

                var asd = new ArrayList<>(List.copyOf(Sets.cartesianProduct(Set.copyOf(permutes),
                        Set.copyOf(pathrelinkingCoeefOptions),
                        Set.copyOf(constructiveHeuristicSettings))));

                Collections.shuffle(asd);


                var sols = new ArrayList<List<Object>>();
                for (var comb : asd) {
                    List<String> permute = (List<String>) comb.get(0);
                    PathRelinkingSettings pathRelinkingSettings = (PathRelinkingSettings)
         comb.get(1);
                    ConstructiveHeuristicSettings constructiveHeuristicSetting =
         (ConstructiveHeuristicSettings) comb.get(2);

                    var random = new Random(0);
                    System.out.println(permute);
                    System.out.println(pathRelinkingSettings.stringIdentifier());
                    System.out.println(constructiveHeuristicSetting.getStringIdentifier());
                    var grasp =
                            new GraspWithPathRelinking(
                                    parameters,
                                    new GraspSettings(
                                            SearchMode.FIRST_IMPROVEMENT,
                                            60,
                                            new LocalSearchSettings(permute),
                                            constructiveHeuristicSetting,
                                            random,
                                            new AlphaGeneratorUniform(random, 0.1, 1),
                                            1,
                                            "instances/1.json",
                                    pathRelinkingSettings));

                    sols.add(List.of(comb, grasp.getSolution().getBestSolution().revenue));
                }

                sols.sort(Comparator.comparing(i -> (int) i.get(1)));

                BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
                for (var sol : sols) {
                    var comb = (List<Object>) sol.get(0);
                    List<String> permute = (List<String>) comb.get(0);
                    PathRelinkingSettings pathRelinkingSettings = (PathRelinkingSettings)
         comb.get(1);
                    ConstructiveHeuristicSettings constructiveHeuristicSetting =
         (ConstructiveHeuristicSettings) comb.get(2);

                    writer.write(Arrays.toString(permute.toArray()));
                    writer.write("\t" + pathRelinkingSettings.stringIdentifier());
                    writer.write("\t" + constructiveHeuristicSetting.getStringIdentifier());

                    int revenue = (int) sol.get(1);
                    writer.write("\t " + (int) revenue + System.lineSeparator());
                    System.out.println((int) revenue);
                }
                writer.close();

//        var localSearchSettings =
//                new LocalSearchSettings(
//                        List.of(
//                                "shift",
//                                "transfer",
//                                "outOfPool",
//                                "interSwap",
//                                "insert",
//                                "intraSwap"));
//
//        var constructiveHeuristicSettings = new ConstructiveHeuristicSettings(1, 5);
//        var random = new Random();
//        var alphaGenerator = new AlphaGeneratorUniform(random, 0.1, 1);
//        //        var alphaGenerator = new AlphaGeneratorConstant(1);
//
//        var graspSettings =
//                new GraspSettings(
//                        SearchMode.FIRST_IMPROVEMENT,
//                        300,
//                        localSearchSettings,
//                        constructiveHeuristicSettings,
//                        random,
//                        alphaGenerator,
//                        1,
//                        "instances/10.json",
//                        new PathRelinkingSettings(1));
//
//        var grasp = new GraspWithPathRelinking(parameters, graspSettings);
    }
}
