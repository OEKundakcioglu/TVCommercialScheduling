package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;

import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;

import runParameters.GraspSettings;

import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.pathLinking.MixedPathRelinking;
import solvers.heuristicSolvers.grasp.pathLinking.PathRelinkingUtils;

import java.util.*;

public class GraspWithPathRelinking {
    @SuppressWarnings("FieldCanBeLocal")
    private final int eliteSolutionsSize = 5;

    private final ProblemParameters parameters;
    private final List<Solution> eliteSolutions;
    private final Random random;
    private final GraspSettings graspSettings;
    private final GraspInformation graspInformation;
    private final List<CheckPoint> checkPoints;
    private final SolverSolution solverSolution;
    private final PathRelinkingUtils pathRelinkingUtils;
    private Solution bestSolution;
    private int foundSolutionAt;
    private int noImprovementCounter = 0;

    public GraspWithPathRelinking(ProblemParameters parameters, GraspSettings graspSettings)
            throws Exception {
        random = graspSettings.random();

        this.parameters = parameters;
        this.eliteSolutions = new LinkedList<>();
        this.graspSettings = graspSettings;

        this.graspInformation =
                new GraspInformation(
                        parameters.getSetOfCommercials(),
                        parameters.getSetOfInventories(),
                        graspSettings);
        this.checkPoints = new ArrayList<>();
        this.pathRelinkingUtils =
                new PathRelinkingUtils(
                        random,
                        graspSettings.localSearchSettings(),
                        graspSettings.pathRelinkingSettings());

        this.solve();

        this.solverSolution =
                new SolverSolution(
                        bestSolution, checkPoints, graspInformation, parameters.getInstance());
    }

    private void solve() throws Exception {
        double iterationsPerSecond = 1;
        this.bestSolution =
                new ConstructiveHeuristic(
                                parameters,
                                this.graspSettings.alphaGenerator().generateAlpha(),
                                random,
                                graspSettings.constructiveHeuristicSettings())
                        .getSolution();

        ProgressBar pb =
                new ProgressBarBuilder()
                        .setUnit("s", 1)
                        .setInitialMax(graspSettings.timeLimit())
                        .hideEta()
                        .setConsumer(new ConsoleProgressBarConsumer(System.out, 120))
                        .setTaskName("Reactive Grasp with Path Relinking")
                        .build();

        var startTime = System.currentTimeMillis() / 1000;

        int iteration = 1;
        while (System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {
            var randomSolution =
                    new ConstructiveHeuristic(
                                    parameters,
                                    this.graspSettings.alphaGenerator().generateAlpha(),
                                    random,
                                    graspSettings.constructiveHeuristicSettings())
                            .getSolution();
            randomSolution =
                    new LocalSearch(
                                    randomSolution,
                                    parameters,
                                    graspSettings.getSearchMode(),
                                    graspSettings.localSearchSettings(),
                                    random)
                            .getSolution();

            if (this.eliteSolutions.size() > 2) {
                var initialSolution = randomSolution;
                var guidingSolution = getGuidingSolution();

                randomSolution =
                        new MixedPathRelinking(
                                        parameters,
                                        initialSolution,
                                        guidingSolution,
                                        pathRelinkingUtils)
                                .getBestFoundSolution();

                randomSolution =
                        new LocalSearch(
                                        randomSolution,
                                        parameters,
                                        graspSettings.getSearchMode(),
                                        graspSettings.localSearchSettings(),
                                        random)
                                .getSolution();
            }

            this.updateEliteSolutions(randomSolution, pb, startTime, iterationsPerSecond);

            pb.stepTo((int) (System.currentTimeMillis() / 1000 - startTime));

            if (iteration % 10 == 0) {
                iterationsPerSecond =
                        iteration / (double) (System.currentTimeMillis() / 1000 - startTime);
            }

            if (noImprovementCounter % 10 == 0) {
                graspSettings.localSearchSettings().randomMoveProbability =
                        Math.min(0.5, 0.1 * (Math.sqrt(noImprovementCounter) / (5)));
            }

            pb.setExtraMessage(
                    String.format(
                            "%.2f iter/s Best solution: %d found at %ds %.2f",
                            iterationsPerSecond,
                            bestSolution.revenue,
                            foundSolutionAt,
                            graspSettings.localSearchSettings().randomMoveProbability));

            noImprovementCounter++;
            iteration++;
        }
        pb.stepTo(graspSettings.timeLimit());
        pb.close();
    }

    private void updateEliteSolutions(
            Solution newFoundLocalOptima,
            ProgressBar pb,
            long startTime,
            double iterationsPerSecond) {
        if (newFoundLocalOptima.revenue > bestSolution.revenue) {
            this.foundSolutionAt = (int) (System.currentTimeMillis() / 1000 - startTime);

            var gain = newFoundLocalOptima.revenue - bestSolution.revenue;
            var gainPercentage = 1e-4;
            //                    Math.min(0.0001, 0.0001 * (iterationsPerSecond * 10 /
            // noImprovementCounter));
            if (gain > bestSolution.revenue * gainPercentage) noImprovementCounter = 0;
            //            noImprovementCounter = 0;

            bestSolution = newFoundLocalOptima;
            pb.setExtraMessage(
                    String.format(
                            "%.2fiter/s %d$ at %ds %.2f %f",
                            iterationsPerSecond,
                            bestSolution.revenue,
                            (System.currentTimeMillis() / 1000 - startTime),
                            graspSettings.localSearchSettings().randomMoveProbability,
                            gainPercentage));

            this.checkPoints.add(
                    new CheckPoint(
                            newFoundLocalOptima,
                            ((double) System.currentTimeMillis() / 1000 - startTime)));
        }

        if (eliteSolutions.size() < eliteSolutionsSize) {
            if (eliteSolutions.isEmpty()) eliteSolutions.add(newFoundLocalOptima);
            else {
                var minDistance =
                        eliteSolutions.stream()
                                .map(
                                        solution ->
                                                pathRelinkingUtils.distance(
                                                        solution, newFoundLocalOptima))
                                .min(Comparator.comparing(distance -> distance))
                                .orElseThrow();
                if (minDistance > 0) eliteSolutions.add(newFoundLocalOptima);
            }
        } else {
            int minDistance =
                    eliteSolutions.stream()
                            .map(
                                    solution ->
                                            pathRelinkingUtils.distance(
                                                    solution, newFoundLocalOptima))
                            .min(Comparator.comparing(distance -> distance))
                            .orElseThrow();

            if (minDistance == 0) return;

            var worstSolution =
                    eliteSolutions.stream()
                            .min(Comparator.comparing(solution -> solution.revenue))
                            .orElseThrow();
            if (minDistance > 0 && newFoundLocalOptima.revenue > worstSolution.revenue) {
                eliteSolutions.remove(worstSolution);
                eliteSolutions.add(newFoundLocalOptima);
            }
        }
    }


    public SolverSolution getSolution() {
        return solverSolution;
    }

    private Solution getGuidingSolution() {
        return eliteSolutions.get(random.nextInt(eliteSolutions.size()));
    }
}
