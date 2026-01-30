package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;
import runParameters.GraspSettings;
import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristicType;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.IConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.RegretBasedConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;
import solvers.heuristicSolvers.grasp.pathLinking.MixedPathRelinking;
import solvers.heuristicSolvers.grasp.pathLinking.PathRelinkingUtils;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;

import java.util.*;

public class GraspWithPathRelinking {
    @SuppressWarnings("FieldCanBeLocal")
    private final int eliteSolutionsSize = 10;

    private final ProblemParameters parameters;
    private final List<Solution> eliteSolutions;
    private final GraspSettings graspSettings;
    private final List<CheckPoint> checkPoints;
    private final SolverSolution solverSolution;
    private final PathRelinkingUtils pathRelinkingUtils;
    private final Random random;
    private Solution bestSolution;
    private int foundSolutionAt;
    private int iterations = 0;
    private double iterationsPerSecond;

    // Aggregated move statistics across all local search calls
    private final MoveStatistics aggregatedMoveStatistics;

    public GraspWithPathRelinking(ProblemParameters parameters, GraspSettings graspSettings)
            throws Exception {

        this.parameters = parameters;
        this.eliteSolutions = new LinkedList<>();
        this.graspSettings = graspSettings;
        this.random = new Random(graspSettings.seed());

        this.checkPoints = new ArrayList<>();
        this.pathRelinkingUtils = new PathRelinkingUtils();

        // Initialize aggregated move statistics if tracking is enabled
        this.aggregatedMoveStatistics = graspSettings.localSearchSettings().trackStatistics
                ? new MoveStatistics() : null;

        this.solve();

        GraspInformation graspInformation =
                new GraspInformation(graspSettings, iterationsPerSecond, aggregatedMoveStatistics);

        this.solverSolution =
                new SolverSolution(
                        bestSolution, checkPoints, graspInformation, parameters.getInstance());
    }

    private void solve() throws Exception {
        this.bestSolution = constructSolution(
                this.graspSettings.alphaGenerator().generateAlpha(this.random));

        var startTime = System.currentTimeMillis() / 1000;

        int iteration = 1;

        while (System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {
            // Generate alpha and create solution
            var alpha = this.graspSettings.alphaGenerator().generateAlpha(this.random);
            var randomSolution = constructSolution(alpha);

            // Provide feedback to reactive alpha generator if applicable
            if (graspSettings.alphaGenerator() instanceof AlphaGeneratorReactive reactive) {
                reactive.updateFeedback(alpha, randomSolution.revenue);
            }

            randomSolution =
                    new LocalSearch(
                            randomSolution,
                            parameters,
                            graspSettings.getSearchMode(),
                            graspSettings.localSearchSettings(),
                            this.random,
                            aggregatedMoveStatistics)
                            .getSolution();

            if (this.eliteSolutions.size() > 2) {
                var initialSolution = randomSolution;
                var guidingSolution = getGuidingSolution();

                randomSolution =
                        new MixedPathRelinking(
                                parameters,
                                initialSolution,
                                guidingSolution,
                                pathRelinkingUtils,
                                this.random)
                                .getBestFoundSolution();

                randomSolution =
                        new LocalSearch(
                                randomSolution,
                                parameters,
                                graspSettings.getSearchMode(),
                                graspSettings.localSearchSettings(),
                                this.random,
                                aggregatedMoveStatistics)
                                .getSolution();
            }

            this.updateEliteSolutions(randomSolution, startTime);

            if (iteration % 10 == 0) {
                iterationsPerSecond =
                        iteration / (double) (System.currentTimeMillis() / 1000 - startTime);
                System.out.printf(
                        "Seconds: %d, Iteration: %d, Iteration per second: %f, Best solution: %d found at %ds%n",
                        System.currentTimeMillis() / 1000 - startTime,
                        iteration,
                        iterationsPerSecond,
                        bestSolution.revenue,
                        foundSolutionAt);
            }

            iteration++;
            iterations++;
        }

        var endTime = System.currentTimeMillis() / 1000;

        iterationsPerSecond = iterations / (double) (endTime - startTime);
    }

    private boolean updateEliteSolutions(Solution newFoundLocalOptima, long startTime) {
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

            if (minDistance == 0) return false;

            var worstSolution =
                    eliteSolutions.stream()
                            .min(Comparator.comparing(solution -> solution.revenue))
                            .orElseThrow();
            if (minDistance > 0 && newFoundLocalOptima.revenue > worstSolution.revenue) {
                eliteSolutions.remove(worstSolution);
                eliteSolutions.add(newFoundLocalOptima);
                return true;
            }
        }

        return false;
    }

    public SolverSolution getSolution() {
        return solverSolution;
    }

    /**
     * Get the aggregated move statistics from all local search calls.
     * Returns null if statistics tracking was not enabled.
     */
    public MoveStatistics getMoveStatistics() {
        return aggregatedMoveStatistics;
    }

    /**
     * Get the iterations per second achieved during the run.
     */
    public double getIterationsPerSecond() {
        return iterationsPerSecond;
    }

    private Solution getGuidingSolution() {
        return eliteSolutions.get(random.nextInt(eliteSolutions.size()));
    }

    /**
     * Construct a solution using the configured constructive heuristic type.
     *
     * @param alpha the alpha parameter for the RCL
     * @return constructed solution
     */
    private Solution constructSolution(double alpha) {
        IConstructiveHeuristic heuristic;
        var settings = graspSettings.constructiveHeuristicSettings();

        if (settings.constructiveHeuristicType() == ConstructiveHeuristicType.REGRET_BASED) {
            heuristic = new RegretBasedConstructiveHeuristic(
                    parameters, alpha, settings, this.random);
        } else {
            heuristic = new ConstructiveHeuristic(
                    parameters, alpha, settings, this.random);
        }

        return heuristic.getSolution();
    }
}
