package scheduling.solver.heuristic.grasp.construction;

import java.util.Arrays;
import java.util.Random;

public class ReactiveAlphaGenerator {

    private static final double MIN_PROBABILITY = 0.05;
    private static final double[] DEFAULT_ALPHAS = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};

    private final double[] alphaValues;
    private final double[] probabilities;
    private final double[] totalQuality;

    public ReactiveAlphaGenerator() {
        this(DEFAULT_ALPHAS);
    }

    public ReactiveAlphaGenerator(double[] alphaValues) {
        this.alphaValues = alphaValues.clone();
        var n = alphaValues.length;
        this.probabilities = new double[n];
        this.totalQuality = new double[n];
        Arrays.fill(probabilities, 1.0 / n);
    }

    public double generateAlpha(Random random) {
        var rand = random.nextDouble();
        var cumulative = 0.0;

        for (int i = 0; i < alphaValues.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return alphaValues[i];
            }
        }

        return alphaValues[alphaValues.length - 1];
    }

    public void feedback(double alpha, double quality) {
        var idx = findAlphaIndex(alpha);
        totalQuality[idx] += quality;
    }

    public void update() {
        recalculateProbabilities();
    }

    private int findAlphaIndex(double alpha) {
        for (int i = 0; i < alphaValues.length; i++) {
            if (Math.abs(alphaValues[i] - alpha) < 0.001) {
                return i;
            }
        }
        throw new IllegalArgumentException("Alpha not found: " + alpha);
    }

    private void recalculateProbabilities() {
        var totalGain = Arrays.stream(totalQuality).sum();

        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] =
                    MIN_PROBABILITY
                            + (1 - MIN_PROBABILITY * totalQuality.length)
                                    * totalQuality[i]
                                    / totalGain;
        }
    }
}
