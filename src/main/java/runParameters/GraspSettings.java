package runParameters;

import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.util.Random;

public class GraspSettings {
    private final boolean isBestMove;
    private final int timeLimit;
    private final LocalSearchSettings localSearchSettings;
    private final ConstructiveHeuristicSettings constructiveHeuristicSettings;
    private transient final Random random;
    private final AlphaGenerator alphaGenerator;
    private final int randomRunN;
    private final String instancePath;

    // Constructor
    public GraspSettings(boolean isBestMove, int timeLimit, LocalSearchSettings localSearchSettings,
                         ConstructiveHeuristicSettings constructiveHeuristicSettings, Random random,
                         AlphaGenerator alphaGenerator, int randomRunN, String instancePath) {
        this.isBestMove = isBestMove;
        this.timeLimit = timeLimit;
        this.localSearchSettings = localSearchSettings;
        this.constructiveHeuristicSettings = constructiveHeuristicSettings;
        this.random = random;
        this.alphaGenerator = alphaGenerator;
        this.randomRunN = randomRunN;
        this.instancePath = instancePath;

        this.random.setSeed(hashCode());

        if (alphaGenerator instanceof AlphaGeneratorUniform) {
            ((AlphaGeneratorUniform) alphaGenerator).setRandom(random);
        }
    }

    // Getters similar to record
    public boolean isBestMove() {
        return isBestMove;
    }

    public int timeLimit() {
        return timeLimit;
    }

    public LocalSearchSettings localSearchSettings() {
        return localSearchSettings;
    }

    public ConstructiveHeuristicSettings constructiveHeuristicSettings() {
        return constructiveHeuristicSettings;
    }

    public Random random() {
        return random;
    }

    public AlphaGenerator alphaGenerator() {
        return alphaGenerator;
    }

    public String getStringIdentifier() {
        return String.format(
                "isBestMove=%b_timeLimit=%ds_seed=%d_alphaGenerator=%s",
                isBestMove,
                timeLimit,
                hashCode(),
                alphaGenerator.getStringIdentifier()
        );
    }

    @Override
    public int hashCode() {
        return String.format(
                "%s_%d_%d_%d_%d_%d",
                isBestMove,
                localSearchSettings.hashCode(),
                constructiveHeuristicSettings.hashCode(),
                alphaGenerator.hashCode(),
                randomRunN,
                instancePath.hashCode()
        ).hashCode();
    }
}


