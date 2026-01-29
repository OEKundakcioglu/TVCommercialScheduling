package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;
    public double neighborhoodSkipProbability;
    public boolean useAdaptiveMoveSelection;
    public boolean trackStatistics;

    public LocalSearchSettings(List<String> moves, double neighborhoodSkipProbability) {
        this(moves, neighborhoodSkipProbability, false, false);
    }

    public LocalSearchSettings(List<String> moves, double neighborhoodSkipProbability,
                               boolean useAdaptiveMoveSelection) {
        this(moves, neighborhoodSkipProbability, useAdaptiveMoveSelection, false);
    }

    public LocalSearchSettings(List<String> moves, double neighborhoodSkipProbability,
                               boolean useAdaptiveMoveSelection, boolean trackStatistics) {
        this.moves = moves;
        this.neighborhoodSkipProbability = neighborhoodSkipProbability;
        this.useAdaptiveMoveSelection = useAdaptiveMoveSelection;
        this.trackStatistics = trackStatistics;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return "neighborhoodSkipProbability=" + neighborhoodSkipProbability +
                "_adaptiveMoves=" + useAdaptiveMoveSelection;
    }
}
