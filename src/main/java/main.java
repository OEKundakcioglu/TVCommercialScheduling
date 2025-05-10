import data.Utils;
import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;
import data.problemBuilders.JsonParser;
import data.problemBuilders.RandomProblemGenerator;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.util.Pair;

import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import runParameters.RandomGeneratorConfig;

import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class main {
    public static void main(String[] args) throws Exception {
        var problemData = new JsonParser().readData("instances/1.json");

        int seed = 0;
        var rng = new JDKRandomGenerator(0);
        int nCommercial = 10;
        int nInventory = 3000;
        var ratingDist = new UniformRealDistribution(rng, 0.1, 4);
        var commercialDurationDist = new UniformRealDistribution(rng, 1, 10);
        var inventoryDurationDist = new UniformRealDistribution(rng, 40, 150);
        var pprDist = new UniformRealDistribution(rng, 0.1, 1);
        var fixedDist = new UniformRealDistribution(rng, 0.1, 1);
        var pricingTypeDist =
                new EnumeratedDistribution<PRICING_TYPE>(rng,
                        List.of(
                                Pair.create(PRICING_TYPE.PRR, 0.5),
                                Pair.create(PRICING_TYPE.FIXED, 0.5)));
        var hoursDist = new EnumeratedDistribution<>(rng, getBalancedPdf(List.of(1, 2, 3, 4)));
        var attentionDistribution =
                new EnumeratedDistribution<>(rng,
                        List.of(
                                Pair.create(ATTENTION.NONE, 0.2),
                                Pair.create(ATTENTION.F30, 0.2),
                                Pair.create(ATTENTION.F60, 0.2),
                                Pair.create(ATTENTION.LAST, 0.2),
                                Pair.create(ATTENTION.FIRST, 0.2)));
        var audienceTypeDist =
                new EnumeratedDistribution<>(
                        rng,
                        getBalancedPdf(IntStream.range(0, 10).boxed().toList()));
        var groupDistribution =
                new EnumeratedDistribution<>(
                        rng,
                        getBalancedPdf(IntStream.range(0, 10).boxed().toList()));

        var invSuitabilityDist = new BinomialDistribution(rng, 1, 0.5);

        var randomGeneratorConfig =
                new RandomGeneratorConfig(
                        seed,
                        nCommercial,
                        nInventory,
                        ratingDist,
                        commercialDurationDist,
                        inventoryDurationDist,
                        pprDist,
                        fixedDist,
                        pricingTypeDist,
                        hoursDist,
                        attentionDistribution,
                        audienceTypeDist,
                        groupDistribution,
                        invSuitabilityDist);
        problemData = new RandomProblemGenerator(randomGeneratorConfig).generate();

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

    private static <T> List<Pair<T, Double>> getBalancedPdf(List<T> items) {
        double singleProb = 1.0 / items.size();
        return items.stream().map(item -> new Pair<>(item, singleProb)).toList();
    }
}
