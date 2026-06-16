package scheduling.solver.heuristic.grasp.vnd.statistics;

import lombok.Getter;

@Getter
public class PhaseStatistics {

    private int callCount;
    private long totalTimeNanos;
    private double totalRevenueGain;

    public void record(long timeNanos, double revenueGain) {
        callCount++;
        totalTimeNanos += timeNanos;
        totalRevenueGain += revenueGain;
    }

    public void merge(PhaseStatistics other) {
        callCount += other.callCount;
        totalTimeNanos += other.totalTimeNanos;
        totalRevenueGain += other.totalRevenueGain;
    }
}
