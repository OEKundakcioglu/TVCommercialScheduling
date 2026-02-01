package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;
    public double neighborhoodSkipProbability;

    public double minProbability;
    public int updateProbabilitiesAtEveryNIter;

    public boolean trackStatistics;

    public LocalSearchSettings(List<String> moves, double neighborhoodSkipProbability) {
        this(moves, neighborhoodSkipProbability, false, 0, 0);
    }

    public LocalSearchSettings(
            List<String> moves,
            double neighborhoodSkipProbability,
            boolean trackStatistics,
            double minProbability,
            int updateProbabilitiesAtEveryNIter) {
        this.moves = moves;
        this.neighborhoodSkipProbability = neighborhoodSkipProbability;
        this.trackStatistics = trackStatistics;
        this.minProbability = minProbability;
        this.updateProbabilitiesAtEveryNIter = updateProbabilitiesAtEveryNIter;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return "neighborhoodSkipProbability=" + neighborhoodSkipProbability;
    }
}
