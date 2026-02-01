package solvers.heuristicSolvers.grasp;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks efficacy statistics for GRASP algorithm components. Records execution time, call counts,
 * and revenue improvements for each component.
 */
public class ComponentEfficacy {

    private final Map<Component, ComponentStats> stats = new EnumMap<>(Component.class);

    public ComponentEfficacy() {
        for (Component component : Component.values()) {
            stats.put(component, new ComponentStats());
        }
    }

    /** Record a component call with its execution time and revenue change. */
    public void recordCall(
            Component component, long timeNanos, double revenueBefore, double revenueAfter) {
        ComponentStats s = stats.get(component);
        s.callCount++;
        s.totalTimeNanos += timeNanos;
        s.totalRevenueGain += (revenueAfter - revenueBefore);
    }

    /** Merge statistics from another instance (for parallel execution). */
    public void merge(ComponentEfficacy other) {
        for (Component component : Component.values()) {
            ComponentStats myStats = this.stats.get(component);
            ComponentStats otherStats = other.stats.get(component);
            myStats.callCount += otherStats.callCount;
            myStats.totalTimeNanos += otherStats.totalTimeNanos;
            myStats.totalRevenueGain += otherStats.totalRevenueGain;
        }
    }

    public enum Component {
        CONSTRUCTIVE_HEURISTIC,
        LOCAL_SEARCH,
        PATH_RELINKING;
    }

    private static class ComponentStats {
        int callCount = 0;
        long totalTimeNanos = 0;
        double totalRevenueGain = 0.0;
    }
}
