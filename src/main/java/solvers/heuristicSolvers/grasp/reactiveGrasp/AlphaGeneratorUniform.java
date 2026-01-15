package solvers.heuristicSolvers.grasp.reactiveGrasp;

public class AlphaGeneratorUniform implements AlphaGenerator {
    private final double lowerBound;
    private final double upperBound;

    public AlphaGeneratorUniform(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public double generateAlpha(java.util.Random random) {
        return random.nextDouble(lowerBound, upperBound);
    }

    public String getStringIdentifier() {
        return String.format(
                "Uniform_lowerBound=%f_upperBound=%f", this.lowerBound, this.upperBound);
    }
}
