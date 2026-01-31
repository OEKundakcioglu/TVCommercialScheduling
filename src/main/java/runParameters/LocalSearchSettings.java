package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;
    public double neighborhoodSkipProbability;

    public boolean useAdaptiveMoveSelection;
    public double minProbability;
    public int updateProbabilitiesAtEveryNIter;

    public boolean trackStatistics;

    public LocalSearchSettings(List<String> moves, double neighborhoodSkipProbability) {
        this(moves, neighborhoodSkipProbability, false, false, 0, 0);
    }

    public LocalSearchSettings(
            List<String> moves,
            double neighborhoodSkipProbability,
            boolean useAdaptiveMoveSelection,
            double minProbability,
            int updateProbabilitiesAtEveryNIter) {
        this(
                moves,
                neighborhoodSkipProbability,
                useAdaptiveMoveSelection,
                false,
                minProbability,
                updateProbabilitiesAtEveryNIter);
    }

    public LocalSearchSettings(
            List<String> moves,
            double neighborhoodSkipProbability,
            boolean useAdaptiveMoveSelection,
            boolean trackStatistics,
            double minProbability,
            int updateProbabilitiesAtEveryNIter) {
        this.moves = moves;
        this.neighborhoodSkipProbability = neighborhoodSkipProbability;
        this.useAdaptiveMoveSelection = useAdaptiveMoveSelection;
        this.trackStatistics = trackStatistics;
        this.minProbability = minProbability;
        this.updateProbabilitiesAtEveryNIter = updateProbabilitiesAtEveryNIter;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return "neighborhoodSkipProbability="
                + neighborhoodSkipProbability
                + "_adaptiveMoves="
                + useAdaptiveMoveSelection;
    }
}
