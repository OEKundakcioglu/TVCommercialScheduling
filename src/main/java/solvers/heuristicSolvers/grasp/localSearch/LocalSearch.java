package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;
import runParameters.LocalSearchSettings;

import java.util.Arrays;
import java.util.Random;

public class LocalSearch {
    private Solution bestFoundSolution;
    private final Solution currentSolution;
    private final ProblemParameters parameters;
    private final LocalSearchSettings localSearchSettings;

    private final Random random;

    private static final double LEARNING_RATE = 0.1;
    private static final double MIN_PROBABILITY = 0.05; // Minimum probability to keep moves viable
    // Move statistics tracking
    private final MoveStatistics moveStatistics;
    // Adaptive move selection parameters
    private double[] moveProbabilities;

    public LocalSearch(
            Solution currentSolution,
            ProblemParameters parameters,
            SearchMode searchMode,
            LocalSearchSettings localSearchSettings,
            Random random)
            throws Exception {
        this(currentSolution, parameters, searchMode, localSearchSettings, random, null);
    }

    public LocalSearch(
            Solution currentSolution,
            ProblemParameters parameters,
            SearchMode searchMode,
            LocalSearchSettings localSearchSettings,
            Random random,
            MoveStatistics externalStats)
            throws Exception {
        this.currentSolution = currentSolution;
        this.bestFoundSolution = currentSolution;
        this.parameters = parameters;
        this.localSearchSettings = localSearchSettings;
        this.random = random;
        this.moveStatistics = (externalStats != null) ? externalStats :
                (localSearchSettings.trackStatistics ? new MoveStatistics() : null);

        if (localSearchSettings.useAdaptiveMoveSelection) {
            this.searchAdaptive(searchMode);
        } else {
            this.search(searchMode);
        }
    }

    /**
     * Original search method - fixed move sequence with VND-style cycling.
     */
    private void search(SearchMode searchMode) throws Exception {
        int k = 0;
        this.bestFoundSolution = currentSolution;

        var solution = currentSolution;
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
    }

    /**
     * Adaptive search method - probability-based move selection with learning.
     * Increases probability for successful moves and decreases for unsuccessful ones.
     */
    private void searchAdaptive(SearchMode searchMode) throws Exception {
        initUniformProbabilities();
        this.bestFoundSolution = currentSolution;
        var solution = currentSolution;

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

            var newSolution = applySearch(moveString, solution, searchMode);

            if (moveStatistics != null) {
                moveStatistics.recordTime(moveString, System.nanoTime() - startTime);
                // Record success only if the move actually improved the solution
                if (newSolution.revenue > prevRevenue) {
                    moveStatistics.recordSuccess(moveString, newSolution.revenue - prevRevenue);
                }
            }

            if (newSolution.revenue > bestFoundSolution.revenue) {
                // Successful move - increase its probability
                moveProbabilities[moveIdx] *= (1 + LEARNING_RATE);
                normalizeProbabilities();
                noImprovement = 0;
                bestFoundSolution = newSolution;
                solution = newSolution;
            } else {
                // Unsuccessful move - slightly decrease its probability
                moveProbabilities[moveIdx] *= (1 - LEARNING_RATE * 0.1);
                normalizeProbabilities();
                noImprovement++;
            }
        }
    }

    /**
     * Initialize uniform probabilities for all moves.
     */
    private void initUniformProbabilities() {
        int numMoves = localSearchSettings.moves.size();
        moveProbabilities = new double[numMoves];
        double initialProb = 1.0 / numMoves;
        Arrays.fill(moveProbabilities, initialProb);
    }

    /**
     * Select a move index based on current probabilities.
     */
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

    /**
     * Normalize probabilities to sum to 1, ensuring minimum probability.
     */
    private void normalizeProbabilities() {
        // Apply minimum probability
        for (int i = 0; i < moveProbabilities.length; i++) {
            if (moveProbabilities[i] < MIN_PROBABILITY) {
                moveProbabilities[i] = MIN_PROBABILITY;
            }
        }

        // Normalize
        double sum = 0.0;
        for (double p : moveProbabilities) {
            sum += p;
        }
        for (int i = 0; i < moveProbabilities.length; i++) {
            moveProbabilities[i] /= sum;
        }
    }

    @SuppressWarnings("IfCanBeSwitch")
    public Solution applySearch(String key, Solution solution, SearchMode searchMode) throws Exception {
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
        else if (key.equals("chainSwap"))
            search = new ChainSwapSearch(solution, parameters, false, searchMode, random);
        else throw new RuntimeException("Invalid key: " + key);

        return search.getSolution();
    }

    public Solution getSolution() {
        return bestFoundSolution;
    }

    /**
     * Get the move statistics collected during this local search.
     * Returns null if statistics tracking was not enabled.
     */
    public MoveStatistics getMoveStatistics() {
        return moveStatistics;
    }
}
