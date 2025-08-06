package solvers.heuristicSolvers.grasp.reactiveGrasp;

public class AlphaGeneratorConstant implements AlphaGenerator {
    private final double alpha;

    public AlphaGeneratorConstant(double alpha) {
        this.alpha = alpha;
    }

    public double generateAlpha() {
        return alpha;
    }

    public String getStringIdentifier(){
        return String.format(
                "Constant_alpha_%f",
                this.alpha
        );
    }
}
