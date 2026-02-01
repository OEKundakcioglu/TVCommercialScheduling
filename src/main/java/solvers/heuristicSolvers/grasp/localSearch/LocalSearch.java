package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;

import runParameters.LocalSearchSettings;

import java.util.Arrays;
import java.util.Random;

public class LocalSearch {
    private final ProblemParameters parameters;
    private final LocalSearchSettings localSearchSettings;
    private final SearchMode searchMode;
    private final Random random;

    private static final double LEARNING_RATE = 0.1;
    private static final double MIN_PROBABILITY = 0.05; // Minimum probability to keep moves viable
    // Move statistics tracking
    private final MoveStatistics moveStatistics;
    // Adaptive move selection parameters - persists across search() calls
    private double[] moveProbabilities;
    private double[] moveGains;

    private int iterations = 0;

    /**
     * Reusable constructor - creates a LocalSearch instance that can be reused across multiple
     * search() calls. Move probabilities are initialized once and persist across calls (key for
     * adaptive search).
     */
    public LocalSearch(
            ProblemParameters parameters,
            SearchMode searchMode,
            LocalSearchSettings localSearchSettings,
            Random random,
            MoveStatistics externalStats) {
        this.parameters = parameters;
        this.searchMode = searchMode;
        this.localSearchSettings = localSearchSettings;
        this.random = random;
        this.moveStatistics =
                (externalStats != null)
                        ? externalStats
                        : (localSearchSettings.trackStatistics ? new MoveStatistics() : null);

        // Initialize probabilities ONCE - they persist across all search() calls
        if (localSearchSettings.useAdaptiveMoveSelection) {
            int numMoves = localSearchSettings.moves.size();
            moveProbabilities = new double[numMoves];
            double initialProb = 1.0 / numMoves;
            Arrays.fill(moveProbabilities, initialProb);
            moveGains = new double[numMoves];
        }
    }

    /**
     * Execute local search on the given solution. For adaptive mode, moveProbabilities are updated
     * and persist to next call.
     *
     * @param startingSolution The solution to improve
     * @return The best found solution after local search
     */
    public Solution search(Solution startingSolution) throws Exception {
        Solution solution;
        if (localSearchSettings.useAdaptiveMoveSelection) {
            solution = searchAdaptive(startingSolution);
        } else {
            solution = searchVND(startingSolution);
        }

        iterations++;
        if (iterations % localSearchSettings.updateProbabilitiesAtEveryNIter == 0)
            updateProbabilities();

        return solution;
    }

    /** VND-style search - fixed move sequence with cycling back on improvement. */
    private Solution searchVND(Solution startingSolution) throws Exception {
        int k = 0;
        Solution bestFoundSolution = startingSolution;
        Solution solution = startingSolution;

        while (k < localSearchSettings.moves.size()) {
            if (this.random.nextDouble() < localSearchSettings.neighborhoodSkipProbability) {
                k++;
                continue;
            }

            var moveString = localSearchSettings.moves.get(k);
            double prevRevenue = solution.revenue;
            long startTime = moveStatistics != null ? System.nanoTime() : 0;

            if (moveStatistics != null) {
                moveStatistics.recordAttempt(moveString);
            }

            solution = applySearch(moveString, solution, searchMode);

            if (moveStatistics != null) {
                moveStatistics.recordTime(moveString, System.nanoTime() - startTime);
                // Record success only if the move actually improved the solution
                if (solution.revenue > prevRevenue) {
                    moveStatistics.recordSuccess(moveString, solution.revenue - prevRevenue);
                }
            }

            if (solution.revenue > bestFoundSolution.revenue) {
                k = 0;
                bestFoundSolution = solution;
            } else {
                k++;
            }
        }
        return bestFoundSolution;
    }

    /**
     * Adaptive search method - probability-based move selection with learning. Increases
     * probability for successful moves and decreases for unsuccessful ones. Note: moveProbabilities
     * persist across calls (initialized once in constructor).
     */
    private Solution searchAdaptive(Solution startingSolution) throws Exception {
        Solution bestFoundSolution = startingSolution;
        Solution solution = startingSolution;

        int noImprovement = 0;
        int maxNoImprovement = localSearchSettings.moves.size() * 2;

        while (noImprovement < maxNoImprovement) {
            // Skip with probability (same as original)
            if (this.random.nextDouble() < localSearchSettings.neighborhoodSkipProbability) {
                noImprovement++;
                continue;
            }

            int moveIdx = selectMoveByProbability();
            var moveString = localSearchSettings.moves.get(moveIdx);
            double prevRevenue = solution.revenue;
            long startTime = moveStatistics != null ? System.nanoTime() : 0;

            if (moveStatistics != null) {
                moveStatistics.recordAttempt(moveString);
            }

            solution = applySearch(moveString, solution, searchMode);
            moveGains[moveIdx] += solution.revenue - prevRevenue;

            if (moveStatistics != null) {
                moveStatistics.recordTime(moveString, System.nanoTime() - startTime);
                // Record success only if the move actually improved the solution
                if (solution.revenue > prevRevenue) {
                    moveStatistics.recordSuccess(moveString, solution.revenue - prevRevenue);
                }
            }

            if (solution.revenue > bestFoundSolution.revenue) {
                noImprovement = 0;
                bestFoundSolution = solution;
            } else {
                noImprovement++;
            }
        }
        return bestFoundSolution;
    }

    /** Select a move index based on current probabilities. */
    private int selectMoveByProbability() {
        double rand = random.nextDouble();
        double cumulative = 0.0;
        for (int i = 0; i < moveProbabilities.length; i++) {
            cumulative += moveProbabilities[i];
            if (rand <= cumulative) {
                return i;
            }
        }
        return moveProbabilities.length - 1; // Fallback to last move
    }

    @SuppressWarnings("IfCanBeSwitch")
    public Solution applySearch(String key, Solution solution, SearchMode searchMode)
            throws Exception {
        BaseSearch search;

        if (key.equals("insert"))
            search = new InsertSearch(solution, parameters, false, searchMode, random);
        else if (key.equals("transfer"))
            search = new TransferSearch(solution, parameters, false, searchMode, random);
        else if (key.equals("outOfPool"))
            search = new OutOfPoolSwapSearch(solution, parameters, false, searchMode, random);
        else if (key.equals("intraSwap"))
            search = new IntraSwapSearch(solution, parameters, false, searchMode, random);
        else if (key.equals("interSwap"))
            search = new InterSwapSearch(solution, parameters, false, searchMode, random);
        else if (key.equals("shift"))
            search = new ShiftSearch(solution, parameters, false, searchMode, random);
        else throw new RuntimeException("Invalid key: " + key);

        return search.getSolution();
    }

    /**
     * Get the move statistics collected during this local search. Returns null if statistics
     * tracking was not enabled.
     */
    public MoveStatistics getMoveStatistics() {
        return moveStatistics;
    }

    private void updateProbabilities() {
        var totalGain = Arrays.stream(moveGains).sum();

        for (int i = 0; i < moveGains.length; i++) {
            moveProbabilities[i] =
                    localSearchSettings.minProbability
                            + (1 - localSearchSettings.minProbability * moveGains.length)
                                    * moveGains[i]
                                    / totalGain;
        }
    }
}
