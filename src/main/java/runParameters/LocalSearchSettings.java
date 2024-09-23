package runParameters;

import java.util.List;

public class LocalSearchSettings {
    public List<String> moves;

    public LocalSearchSettings(List<String> moves) {
        this.moves = moves;
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return String.join(", ", moves);
    }

    @Override
    public int hashCode() {
        return moves.hashCode();
    }
}
