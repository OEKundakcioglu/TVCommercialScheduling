package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;
    public double randomMoveProbability;

    public LocalSearchSettings(List<String> moves) {
        this.moves = moves;
        this.randomMoveProbability = 0;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return String.join(", ", moves) + ", " + randomMoveProbability;
    }

    @Override
    public int hashCode() {
        return moves.hashCode() + Double.hashCode(randomMoveProbability);
    }
}
