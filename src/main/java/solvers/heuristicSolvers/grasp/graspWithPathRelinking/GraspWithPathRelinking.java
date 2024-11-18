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
    private final int eliteSolutionsSize = 10;

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
                new PathRelinkingUtils(random, graspSettings.localSearchSettings());

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

            pb.setExtraMessage(
                    String.format(
                            "%.2f iter/s Best solution: %d found at %ds %.2f",
                            iterationsPerSecond,
                            bestSolution.revenue,
                            foundSolutionAt,
                            graspSettings.localSearchSettings().randomMoveProbability));

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

            bestSolution = newFoundLocalOptima;

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
