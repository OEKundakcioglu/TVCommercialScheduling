package solvers.heuristicSolvers.grasp.localSearch;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks per-move statistics for local search analysis.
 * Records attempts, successes, feasibility rates, and revenue gains for each move type.
 */
public class MoveStatistics {

    private final Map<String, MoveStats> stats = new LinkedHashMap<>();

    private MoveStats getOrCreate(String moveName) {
        return stats.computeIfAbsent(moveName, k -> new MoveStats());
    }

    /**
     * Record that a move was attempted.
     */
    public void recordAttempt(String moveName) {
        getOrCreate(moveName).attempts++;
    }

    /**
     * Record that a move was successful (improved solution).
     */
    public void recordSuccess(String moveName, double revenueGain) {
        var moveStats = getOrCreate(moveName);
        moveStats.successes++;
        moveStats.totalRevenueGain += revenueGain;
    }

    /**
     * Record time taken for a move search.
     */
    public void recordTime(String moveName, long timeNanos) {
        getOrCreate(moveName).totalTimeNanos += timeNanos;
    }

    /**
     * Merge statistics from another instance (useful for parallel execution).
     */
    public void merge(MoveStatistics other) {
        for (var entry : other.stats.entrySet()) {
            var name = entry.getKey();
            var otherStats = entry.getValue();
            var myStats = getOrCreate(name);
            myStats.attempts += otherStats.attempts;
            myStats.successes += otherStats.successes;
            myStats.totalRevenueGain += otherStats.totalRevenueGain;
            myStats.totalTimeNanos += otherStats.totalTimeNanos;
        }
    }

    /**
     * Check if any statistics have been recorded.
     */
    public boolean isEmpty() {
        return stats.isEmpty();
    }


    /**
     * Internal class to hold statistics for a single move type.
     */
    private static class MoveStats {
        int attempts = 0;
        int successes = 0;
        double totalRevenueGain = 0.0;
        long totalTimeNanos = 0;
    }
}
