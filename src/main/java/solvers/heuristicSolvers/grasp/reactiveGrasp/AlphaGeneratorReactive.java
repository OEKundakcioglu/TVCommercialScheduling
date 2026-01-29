package solvers.heuristicSolvers.grasp.reactiveGrasp;

import java.util.Arrays;
import java.util.Random;

/**
 * Reactive Alpha Generator that learns from solution quality feedback.
 * Tracks average solution quality per alpha value and biases selection
 * toward alpha values that have produced better solutions.
 */
public class AlphaGeneratorReactive implements AlphaGenerator {
    private static final int UPDATE_INTERVAL = 50; // Recalculate probabilities every N iterations
    private static final double MIN_PROBABILITY = 0.05; // Minimum probability for any alpha
    private final double[] alphaValues;
    private final double[] probabilities;
    private final double[] totalQuality;
    private final int[] usageCount;
    private int totalIterations = 0;

    /**
     * Creates a reactive alpha generator with default alpha values.
     */
    public AlphaGeneratorReactive() {
        this(new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9});
    }

    /**
     * Creates a reactive alpha generator with custom alpha values.
     *
     * @param alphaValues Array of alpha values to choose from
     */
    public AlphaGeneratorReactive(double[] alphaValues) {
        this.alphaValues = alphaValues.clone();
        int n = alphaValues.length;
        this.probabilities = new double[n];
        this.totalQuality = new double[n];
        this.usageCount = new int[n];

        // Initialize with uniform probabilities
        double uniformProb = 1.0 / n;
        Arrays.fill(probabilities, uniformProb);
    }

    @Override
    public double generateAlpha(Random random) {
        double rand = random.nextDouble();
        double cumulative = 0.0;

        for (int i = 0; i < alphaValues.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return alphaValues[i];
            }
        }

        return alphaValues[alphaValues.length - 1]; // Fallback
    }

    /**
     * Update feedback after using an alpha value.
     *
     * @param alpha           The alpha value that was used
     * @param solutionQuality The quality (revenue) of the resulting solution
     */
    public void updateFeedback(double alpha, int solutionQuality) {
        int idx = findAlphaIndex(alpha);

        usageCount[idx]++;
        totalQuality[idx] += solutionQuality;
        totalIterations++;

        // Recalculate probabilities periodically
        if (totalIterations % UPDATE_INTERVAL == 0) {
            recalculateProbabilities();
        }
    }

    /**
     * Find the index of an alpha value in our array.
     */
    private int findAlphaIndex(double alpha) {
        for (int i = 0; i < alphaValues.length; i++) {
            if (Math.abs(alphaValues[i] - alpha) < 0.001) {
                return i;
            }
        }

        throw new IllegalArgumentException("Alpha not found in list: " + alpha);
    }

    /**
     * Recalculate selection probabilities based on average quality.
     */
    private void recalculateProbabilities() {
        double[] avgQuality = new double[alphaValues.length];
        double maxAvg = Double.NEGATIVE_INFINITY;
        double minAvg = Double.POSITIVE_INFINITY;

        // Calculate average quality for each alpha
        for (int i = 0; i < alphaValues.length; i++) {
            if (usageCount[i] == 0) {
                avgQuality[i] = 0;
                continue;
            }

            avgQuality[i] = totalQuality[i] / usageCount[i];

            if (avgQuality[i] > maxAvg) maxAvg = avgQuality[i];
            if (avgQuality[i] < minAvg) minAvg = avgQuality[i];
        }

        // Calculate new probabilities proportional to (avgQuality - minAvg)
        double sum = 0.0;
        for (int i = 0; i < alphaValues.length; i++) {
            if (usageCount[i] == 0) {
                probabilities[i] = 0.5 + MIN_PROBABILITY;
            } else {
                probabilities[i] = (avgQuality[i] - minAvg) / (maxAvg - minAvg) + MIN_PROBABILITY;
            }

            sum += probabilities[i];
        }

        // Normalize to sum to 1
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= sum;
        }
    }

    /**
     * Get current probabilities (for debugging/logging).
     */
    public double[] getProbabilities() {
        return probabilities.clone();
    }

    /**
     * Get usage counts (for debugging/logging).
     */
    public int[] getUsageCounts() {
        return usageCount.clone();
    }

    /**
     * Get average quality per alpha (for debugging/logging).
     */
    public double[] getAverageQualities() {
        double[] avgQuality = new double[alphaValues.length];
        for (int i = 0; i < alphaValues.length; i++) {
            if (usageCount[i] > 0) {
                avgQuality[i] = totalQuality[i] / usageCount[i];
            }
        }
        return avgQuality;
    }

    @Override
    public String getStringIdentifier() {
        return "Reactive_nAlphas=" + alphaValues.length;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(alphaValues);
    }
}
