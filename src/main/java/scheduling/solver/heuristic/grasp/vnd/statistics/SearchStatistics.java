package scheduling.solver.heuristic.grasp.vnd.statistics;

import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;

@Getter
public class SearchStatistics {

    private final Map<NeighborhoodType, MoveStatistics> moveStatistics =
            new EnumMap<>(NeighborhoodType.class);
    private int totalIterations;
    private int totalImprovements;

    public MoveStatistics getOrCreateMoveStatistics(NeighborhoodType type) {
        return moveStatistics.computeIfAbsent(type, k -> new MoveStatistics());
    }

    public void recordIteration() {
        totalIterations++;
    }

    public void recordImprovement() {
        totalImprovements++;
    }

    public void merge(SearchStatistics other) {
        this.totalIterations += other.totalIterations;
        this.totalImprovements += other.totalImprovements;
        for (var entry : other.moveStatistics.entrySet()) {
            getOrCreateMoveStatistics(entry.getKey()).merge(entry.getValue());
        }
    }
}
