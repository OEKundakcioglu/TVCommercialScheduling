package solvers.heuristicSolvers.grasp.localSearch;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
     * Record that a move was feasible (passed constraint checks).
     */
    public void recordFeasible(String moveName) {
        getOrCreate(moveName).feasible++;
    }

    /**
     * Record that a move was infeasible (failed constraint checks).
     */
    public void recordInfeasible(String moveName) {
        // Attempt already recorded, nothing extra needed
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
            myStats.feasible += otherStats.feasible;
            myStats.totalRevenueGain += otherStats.totalRevenueGain;
            myStats.totalTimeNanos += otherStats.totalTimeNanos;
        }
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        stats.clear();
    }

    /**
     * Get number of attempts for a move.
     */
    public int getAttempts(String moveName) {
        var moveStats = stats.get(moveName);
        return moveStats != null ? moveStats.attempts : 0;
    }

    /**
     * Get number of successes for a move.
     */
    public int getSuccesses(String moveName) {
        var moveStats = stats.get(moveName);
        return moveStats != null ? moveStats.successes : 0;
    }

    /**
     * Get success rate for a move (percentage).
     */
    public double getSuccessRate(String moveName) {
        var moveStats = stats.get(moveName);
        return moveStats != null ? moveStats.getSuccessRate() : 0.0;
    }

    /**
     * Get feasibility rate for a move (percentage).
     */
    public double getFeasibilityRate(String moveName) {
        var moveStats = stats.get(moveName);
        return moveStats != null ? moveStats.getFeasibilityRate() : 0.0;
    }

    /**
     * Get average revenue gain for successful moves.
     */
    public double getAverageGain(String moveName) {
        var moveStats = stats.get(moveName);
        return moveStats != null ? moveStats.getAverageGain() : 0.0;
    }

    /**
     * Get average time in milliseconds for a move.
     */
    public double getAverageTimeMs(String moveName) {
        var moveStats = stats.get(moveName);
        return moveStats != null ? moveStats.getAverageTimeMs() : 0.0;
    }

    /**
     * Get all move names that have been recorded.
     */
    public Set<String> getMoveNames() {
        return stats.keySet();
    }

    /**
     * Check if any statistics have been recorded.
     */
    public boolean isEmpty() {
        return stats.isEmpty();
    }

    /**
     * Print a summary of all move statistics.
     */
    public void printSummary() {
        System.out.println("\n=== Move Statistics Summary ===");
        System.out.printf("%-12s %8s %8s %8s %12s %8s%n",
                "Move", "Attempts", "Success%", "Feasib%", "Avg Gain", "Avg Time");
        System.out.println("-".repeat(68));

        for (var entry : stats.entrySet()) {
            var name = entry.getKey();
            var s = entry.getValue();
            System.out.printf("%-12s %8d %7.1f%% %7.1f%% %+11.0f %7.1fms%n",
                    name,
                    s.attempts,
                    s.getSuccessRate(),
                    s.getFeasibilityRate(),
                    s.getAverageGain(),
                    s.getAverageTimeMs());
        }
        System.out.println("===============================\n");
    }

    /**
     * Get a formatted summary string (useful for logging).
     */
    public String getSummaryString() {
        var sb = new StringBuilder();
        sb.append("MoveStats{");
        boolean first = true;
        for (var entry : stats.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            var s = entry.getValue();
            sb.append(entry.getKey())
                    .append(String.format("(%.1f%%/%.0f)", s.getSuccessRate(), s.getAverageGain()));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Internal class to hold statistics for a single move type.
     */
    private static class MoveStats {
        int attempts = 0;
        int successes = 0;
        int feasible = 0;
        double totalRevenueGain = 0.0;
        long totalTimeNanos = 0;

        double getSuccessRate() {
            return attempts > 0 ? (double) successes / attempts * 100 : 0.0;
        }

        double getFeasibilityRate() {
            return attempts > 0 ? (double) feasible / attempts * 100 : 0.0;
        }

        double getAverageGain() {
            return successes > 0 ? totalRevenueGain / successes : 0.0;
        }

        double getAverageTimeMs() {
            return attempts > 0 ? totalTimeNanos / attempts / 1_000_000.0 : 0.0;
        }
    }
}
