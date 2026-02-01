package solvers.heuristicSolvers.grasp.graspWithPathRelinking;

import data.ProblemParameters;
import data.Solution;

import runParameters.GraspSettings;

import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.ComponentEfficacy;
import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;
import solvers.heuristicSolvers.grasp.localSearch.LocalSearch;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;
import solvers.heuristicSolvers.grasp.pathLinking.MixedPathRelinking;
import solvers.heuristicSolvers.grasp.pathLinking.PathRelinkingUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Abstract base class for GRASP with Path Relinking implementations. Contains shared helper methods
 * to reduce code duplication between single-threaded and parallel implementations.
 */
public abstract class BaseGrasp {

    protected static final int DEFAULT_ELITE_SOLUTIONS_SIZE = 10;

    // Common configuration
    protected final int eliteSolutionsSize;
    protected final ProblemParameters parameters;
    protected final GraspSettings graspSettings;
    protected final PathRelinkingUtils pathRelinkingUtils;

    // Output
    protected SolverSolution solverSolution;
    protected double iterationsPerSecond;

    protected BaseGrasp(ProblemParameters parameters, GraspSettings graspSettings) {
        this(parameters, graspSettings, DEFAULT_ELITE_SOLUTIONS_SIZE);
    }

    protected BaseGrasp(
            ProblemParameters parameters, GraspSettings graspSettings, int eliteSolutionsSize) {
        this.parameters = parameters;
        this.graspSettings = graspSettings;
        this.eliteSolutionsSize = eliteSolutionsSize;
        this.pathRelinkingUtils = new PathRelinkingUtils();
    }

    // ==================== SOLUTION CREATION HELPERS ====================

    /**
     * Creates a solution using the ConstructiveHeuristic.
     *
     * @param alpha the alpha parameter for the restricted candidate list
     * @param random the random generator to use
     * @return the constructed solution
     */
    protected Solution createConstructiveSolution(double alpha, Random random) throws Exception {
        return new ConstructiveHeuristic(
                        parameters, alpha, graspSettings.constructiveHeuristicSettings(), random)
                .getSolution();
    }

    /**
     * Runs path relinking between two solutions.
     *
     * @param initial the starting solution
     * @param guiding the target/guiding solution
     * @param random the random generator to use
     * @return the best solution found during path relinking
     */
    protected Solution runPathRelinking(Solution initial, Solution guiding, Random random)
            throws Exception {
        return new MixedPathRelinking(parameters, initial, guiding, pathRelinkingUtils, random)
                .getBestFoundSolution();
    }

    // ==================== ELITE POOL LOGIC HELPERS ====================

    /**
     * Calculates the minimum distance from a solution to all solutions in the elite pool.
     *
     * @param solution the solution to measure distance from
     * @param eliteSolutions the elite solution pool
     * @return the minimum distance, or Integer.MAX_VALUE if elite pool is empty
     */
    protected int calculateMinDistanceToElitePool(
            Solution solution, List<Solution> eliteSolutions) {
        if (eliteSolutions.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return eliteSolutions.stream()
                .map(elite -> pathRelinkingUtils.distance(elite, solution))
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }

    /**
     * Finds the worst (lowest revenue) solution in the elite pool.
     *
     * @param eliteSolutions the elite solution pool
     * @return the solution with the lowest revenue
     */
    protected Solution findWorstEliteSolution(List<Solution> eliteSolutions) {
        return eliteSolutions.stream().min(Comparator.comparing(s -> s.revenue)).orElseThrow();
    }

    /**
     * Attempts to add a solution to the elite pool. The solution is added if the pool is not full
     * and the solution is diverse enough, or if the pool is full but the solution is better than
     * the worst and diverse enough.
     *
     * <p>Note: This method modifies the eliteSolutions list directly. Callers are responsible for
     * any necessary synchronization (e.g., holding a write lock in parallel implementations).
     *
     * @param newSolution the solution to potentially add
     * @param eliteSolutions the elite solution pool
     * @param maxSize the maximum size of the elite pool
     * @return true if the solution was added, false otherwise
     */
    protected boolean tryAddToElitePool(
            Solution newSolution, List<Solution> eliteSolutions, int maxSize) {

        if (eliteSolutions.size() < maxSize) {
            if (eliteSolutions.isEmpty()) {
                eliteSolutions.add(newSolution);
                return true;
            }
            int minDistance = calculateMinDistanceToElitePool(newSolution, eliteSolutions);
            if (minDistance > 0) {
                eliteSolutions.add(newSolution);
                return true;
            }
        } else {
            int minDistance = calculateMinDistanceToElitePool(newSolution, eliteSolutions);
            if (minDistance == 0) {
                return false;
            }

            Solution worst = findWorstEliteSolution(eliteSolutions);
            if (newSolution.revenue > worst.revenue) {
                eliteSolutions.remove(worst);
                eliteSolutions.add(newSolution);
                return true;
            }
        }
        return false;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Creates a MoveStatistics instance if statistics tracking is enabled.
     *
     * @return a new MoveStatistics instance, or null if tracking is disabled
     */
    protected MoveStatistics createMoveStatistics() {
        return graspSettings.localSearchSettings().trackStatistics ? new MoveStatistics() : null;
    }

    /**
     * Creates a LocalSearch instance with the configured settings.
     *
     * @param random the random generator to use
     * @param stats the move statistics tracker (may be null)
     * @return a new LocalSearch instance
     */
    protected LocalSearch createLocalSearch(Random random, MoveStatistics stats) {
        return new LocalSearch(
                parameters,
                graspSettings.getSearchMode(),
                graspSettings.localSearchSettings(),
                random,
                stats);
    }

    /**
     * Builds the final SolverSolution from the algorithm results.
     *
     * @param bestSolution the best solution found
     * @param checkPoints the list of checkpoints recorded during the run
     * @param stats the move statistics (may be null)
     * @param efficacy the component efficacy tracker
     * @return the complete SolverSolution
     */
    protected SolverSolution buildSolverSolution(
            Solution bestSolution,
            List<CheckPoint> checkPoints,
            MoveStatistics stats,
            ComponentEfficacy efficacy) {

        GraspInformation graspInformation =
                new GraspInformation(graspSettings, iterationsPerSecond, stats, efficacy);

        return new SolverSolution(
                bestSolution, checkPoints, graspInformation, parameters.getInstance());
    }

    // ==================== PUBLIC API ====================

    public SolverSolution getSolution() {
        return solverSolution;
    }

    public double getIterationsPerSecond() {
        return iterationsPerSecond;
    }

    /**
     * Gets the aggregated move statistics from all local search calls.
     *
     * @return the move statistics, or null if tracking was not enabled
     */
    public abstract MoveStatistics getMoveStatistics();
}
