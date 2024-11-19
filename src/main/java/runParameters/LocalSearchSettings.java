package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;
    public double randomMoveProbability;

    public LocalSearchSettings(List<String> moves, double randomMoveProbability) {
        this.moves = moves;
        this.randomMoveProbability = randomMoveProbability;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return "randomMoveProb_" + randomMoveProbability;
    }

    @Override
    public int hashCode() {
        return moves.hashCode() + Double.hashCode(randomMoveProbability);
    }
}
