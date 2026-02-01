package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;

import runParameters.GraspSettings;

import solvers.CheckPoint;
import solvers.heuristicSolvers.grasp.ComponentEfficacy;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;

import java.util.*;

public class GraspWithPathRelinking extends BaseGrasp {

    private final List<Solution> eliteSolutions;
    private final List<CheckPoint> checkPoints;
    private final Random random;
    private Solution bestSolution;
    private int foundSolutionAt;
    private int iterations = 0;

    // Aggregated move statistics across all local search calls
    private final MoveStatistics aggregatedMoveStatistics;

    // Component efficacy tracking
    private final ComponentEfficacy componentEfficacy = new ComponentEfficacy();

    // Single LocalSearch instance - reused across all iterations (preserves adaptive learning)
    private final LocalSearch localSearch;

    public GraspWithPathRelinking(ProblemParameters parameters, GraspSettings graspSettings)
            throws Exception {
        super(parameters, graspSettings);

        this.eliteSolutions = new LinkedList<>();
        this.random = new Random(graspSettings.seed());
        this.checkPoints = new ArrayList<>();

        // Initialize aggregated move statistics if tracking is enabled
        this.aggregatedMoveStatistics = createMoveStatistics();

        // Create single LocalSearch instance - moveProbabilities persist across all iterations
        this.localSearch = createLocalSearch(this.random, this.aggregatedMoveStatistics);

        this.solve();

        this.solverSolution =
                buildSolverSolution(
                        bestSolution, checkPoints, aggregatedMoveStatistics, componentEfficacy);
    }

    private void solve() throws Exception {
        long chStartTime = System.nanoTime();
        this.bestSolution =
                createConstructiveSolution(
                        graspSettings.alphaGenerator().generateAlpha(this.random), this.random);
        componentEfficacy.recordCall(
                ComponentEfficacy.Component.CONSTRUCTIVE_HEURISTIC,
                System.nanoTime() - chStartTime,
                0,
                bestSolution.revenue);

        var startTime = System.currentTimeMillis() / 1000;

        int iteration = 1;

        while (System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {
            // Generate alpha and create solution
            var alpha = graspSettings.alphaGenerator().generateAlpha(this.random);
            long chLoopStartTime = System.nanoTime();
            var randomSolution =
                    createConstructiveSolution(
                            graspSettings.alphaGenerator().generateAlpha(this.random), this.random);
            componentEfficacy.recordCall(
                    ComponentEfficacy.Component.CONSTRUCTIVE_HEURISTIC,
                    System.nanoTime() - chLoopStartTime,
                    0,
                    randomSolution.revenue);

            double prevRevenue = randomSolution.revenue;
            long lsStartTime = System.nanoTime();
            randomSolution = localSearch.search(randomSolution);
            componentEfficacy.recordCall(
                    ComponentEfficacy.Component.LOCAL_SEARCH,
                    System.nanoTime() - lsStartTime,
                    prevRevenue,
                    randomSolution.revenue);

            if (this.eliteSolutions.size() > 2) {
                var initialSolution = randomSolution;
                var guidingSolution = getGuidingSolution();

                double prPrevRevenue = randomSolution.revenue;
                long prStartTime = System.nanoTime();
                randomSolution = runPathRelinking(initialSolution, guidingSolution, this.random);
                componentEfficacy.recordCall(
                        ComponentEfficacy.Component.PATH_RELINKING,
                        System.nanoTime() - prStartTime,
                        prPrevRevenue,
                        randomSolution.revenue);

                double lsPrevRevenue = randomSolution.revenue;
                long lsPostPrStartTime = System.nanoTime();
                randomSolution = localSearch.search(randomSolution);
                componentEfficacy.recordCall(
                        ComponentEfficacy.Component.LOCAL_SEARCH,
                        System.nanoTime() - lsPostPrStartTime,
                        lsPrevRevenue,
                        randomSolution.revenue);
            }

            // Provide feedback to reactive alpha generator if applicable
            if (graspSettings.alphaGenerator() instanceof AlphaGeneratorReactive reactive) {
                reactive.updateFeedback(alpha, randomSolution.revenue);
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

    private void updateEliteSolutions(Solution newFoundLocalOptima, long startTime) {
        // Update best solution and checkpoint if improved
        if (newFoundLocalOptima.revenue > bestSolution.revenue) {
            this.foundSolutionAt = (int) (System.currentTimeMillis() / 1000 - startTime);
            bestSolution = newFoundLocalOptima;
            this.checkPoints.add(
                    new CheckPoint(
                            newFoundLocalOptima,
                            ((double) System.currentTimeMillis() / 1000 - startTime)));
        }

        // Try to add to elite pool using shared helper
        tryAddToElitePool(newFoundLocalOptima, eliteSolutions, eliteSolutionsSize);
    }

    @Override
    public MoveStatistics getMoveStatistics() {
        return aggregatedMoveStatistics;
    }

    private Solution getGuidingSolution() {
        return eliteSolutions.get(random.nextInt(eliteSolutions.size()));
    }
}
