package runParameters;

import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;

public class GraspSettings {
    private final SearchMode searchMode;
    private final int timeLimit;
    private final LocalSearchSettings localSearchSettings;
    private final ConstructiveHeuristicSettings constructiveHeuristicSettings;
    private final AlphaGenerator alphaGenerator;
    private final int randomRunN;
    private final String instancePath;

    // Constructor
    public GraspSettings(SearchMode searchMode, int timeLimit, LocalSearchSettings localSearchSettings,
                         ConstructiveHeuristicSettings constructiveHeuristicSettings,
                         AlphaGenerator alphaGenerator, int randomRunN, String instancePath) {
        this.searchMode = searchMode;
        this.timeLimit = timeLimit;
        this.localSearchSettings = localSearchSettings;
        this.constructiveHeuristicSettings = constructiveHeuristicSettings;
        this.alphaGenerator = alphaGenerator;
        this.randomRunN = randomRunN;
        this.instancePath = instancePath;
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

    public AlphaGenerator alphaGenerator() {
        return alphaGenerator;
    }

    public String getStringIdentifier() {
        return String.format(
                "isBestMove=%s_timeLimit=%ds_alphaGenerator=%s_localSearch=%s_constructiveHeuristic=%s_randomRun=%d",
                searchMode,
                timeLimit,
                alphaGenerator.getStringIdentifier(),
                localSearchSettings.getStringIdentifier(),
                constructiveHeuristicSettings.getStringIdentifier(),
                randomRunN
        );
    }

}


