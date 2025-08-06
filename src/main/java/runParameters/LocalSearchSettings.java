package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;
    public double neighborhoodSkipProbability;

    public LocalSearchSettings(List<String> moves, double neighborhoodSkipProbability) {
        this.moves = moves;
        this.neighborhoodSkipProbability = neighborhoodSkipProbability;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return "randomMoveProb_" + neighborhoodSkipProbability;
    }

    @Override
    public int hashCode() {
        return moves.hashCode() + Double.hashCode(neighborhoodSkipProbability);
    }
}
