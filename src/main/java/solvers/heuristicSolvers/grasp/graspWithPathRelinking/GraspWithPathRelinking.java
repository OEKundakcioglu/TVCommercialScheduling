package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;
import runParameters.GraspSettings;
import solvers.CheckPoint;
import solvers.GlobalRandom;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.pathLinking.MixedPathRelinking;
import solvers.heuristicSolvers.grasp.pathLinking.PathRelinkingUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class GraspWithPathRelinking {
    @SuppressWarnings("FieldCanBeLocal")
    private final int eliteSolutionsSize = 10;

    private final ProblemParameters parameters;
    private final List<Solution> eliteSolutions;
    private final GraspSettings graspSettings;
    private final List<CheckPoint> checkPoints;
    private final SolverSolution solverSolution;
    private final PathRelinkingUtils pathRelinkingUtils;
    private Solution bestSolution;
    private int foundSolutionAt;

    public GraspWithPathRelinking(ProblemParameters parameters, GraspSettings graspSettings)
            throws Exception {

        this.parameters = parameters;
        this.eliteSolutions = new LinkedList<>();
        this.graspSettings = graspSettings;

        GraspInformation graspInformation = new GraspInformation(
                parameters.getSetOfCommercials(),
                parameters.getSetOfInventories(),
                graspSettings);
        this.checkPoints = new ArrayList<>();
        this.pathRelinkingUtils =
                new PathRelinkingUtils();

        this.solve();

        this.solverSolution =
                new SolverSolution(
                        bestSolution, checkPoints, graspInformation, parameters.getInstance());
    }

    private void solve() throws Exception {
        double iterationsPerSecond;
        this.bestSolution =
                new ConstructiveHeuristic(
                                parameters,
                                this.graspSettings.alphaGenerator().generateAlpha(),
                                graspSettings.constructiveHeuristicSettings())
                        .getSolution();

        var startTime = System.currentTimeMillis() / 1000;

        int iteration = 1;
        while (System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {
            var randomSolution = new ConstructiveHeuristic(
                    parameters,
                    this.graspSettings.alphaGenerator().generateAlpha(),
                    graspSettings.constructiveHeuristicSettings())
                    .getSolution();

            randomSolution =
                    new LocalSearch(
                                    randomSolution,
                                    parameters,
                                    graspSettings.getSearchMode(),
                            graspSettings.localSearchSettings())
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
                                graspSettings.localSearchSettings())
                                .getSolution();
            }

            this.updateEliteSolutions(randomSolution, startTime);

//            if (iteration % 10 == 0) {
//                iterationsPerSecond =
//                        iteration / (double) (System.currentTimeMillis() / 1000 - startTime);
//                System.out.printf("Seconds: %d, Iteration: %d, Iteration per second: %f, Best solution: %d found at %ds%n",
//                        System.currentTimeMillis() / 1000 - startTime, iteration, iterationsPerSecond, bestSolution.revenue, foundSolutionAt);
//            }

            iteration++;
        }
    }

    private void updateEliteSolutions(
            Solution newFoundLocalOptima,
            long startTime) {
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
        return eliteSolutions.get(GlobalRandom.getRandom().nextInt(eliteSolutions.size()));
    }
}
