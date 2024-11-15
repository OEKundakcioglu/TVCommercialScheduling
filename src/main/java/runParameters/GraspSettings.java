package runParameters;

import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.util.Random;

public class GraspSettings {
    private final SearchMode searchMode;
    private final int timeLimit;
    private final LocalSearchSettings localSearchSettings;
    private final ConstructiveHeuristicSettings constructiveHeuristicSettings;
    private transient final Random random;
    private final AlphaGenerator alphaGenerator;
    private final int randomRunN;
    private final String instancePath;

    // Constructor
    public GraspSettings(SearchMode searchMode, int timeLimit, LocalSearchSettings localSearchSettings,
                         ConstructiveHeuristicSettings constructiveHeuristicSettings, Random random,
                         AlphaGenerator alphaGenerator, int randomRunN, String instancePath) {
        this.searchMode = searchMode;
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
    public SearchMode getSearchMode() {
        return searchMode;
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
                "isBestMove=%s_timeLimit=%ds_seed=%d_alphaGenerator=%s",
                searchMode,
                timeLimit,
                hashCode(),
                alphaGenerator.getStringIdentifier()
        );
    }

    @Override
    public int hashCode() {
        return String.format(
                "%d_%d_%d_%d_%d_%d",
                searchMode.hashCode(),
                localSearchSettings.hashCode(),
                constructiveHeuristicSettings.hashCode(),
                alphaGenerator.hashCode(),
                randomRunN,
                instancePath.hashCode()
        ).hashCode();
    }
}


