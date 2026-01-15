package solvers.heuristicSolvers.grasp.reactiveGrasp;

import java.util.Random;

public class AlphaGeneratorConstant implements AlphaGenerator {
    private final double alpha;

    public AlphaGeneratorConstant(double alpha) {
        this.alpha = alpha;
    }

    public double generateAlpha(Random random) {
        return alpha;
    }

    public String getStringIdentifier() {
        return String.format("Constant_alpha=%.2f", this.alpha);
    }
}
