package grasp.reactiveGrasp;

import java.util.Random;

public class AlphaGeneratorUniform implements AlphaGenerator {
    private final double lowerBound;
    private final double upperBound;
    private transient Random random;

    public AlphaGeneratorUniform(Random random, double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.random = random;
    }

    public double generateAlpha() {
        return random.nextDouble(lowerBound, upperBound);
    }

    public String getStringIdentifier(){
        return String.format(
                "Uniform_lowerBound_%f_upperBound_%f",
                this.lowerBound,
                this.upperBound
        );
    }

    @Override
    public int hashCode() {
        return String.format("Uniform_lowerBound_%f_upperBound_%f", this.lowerBound, this.upperBound).hashCode();
    }

    public void setRandom(Random random) {
        this.random = random;
    }
}
