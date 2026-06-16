package scheduling.solver.heuristic.grasp.vnd.statistics;

import lombok.Getter;

@Getter
public class MoveStatistics {

    private int attemptCount;
    private int successCount;
    private long totalTimeNanos;

    private double gainSum;
    private double gainMin;
    private double gainMax;
    private double gainMean;
    private double m2;

    public void recordAttempt(long timeNanos) {
        attemptCount++;
        totalTimeNanos += timeNanos;
    }

    public void recordSuccess(double gain) {
        successCount++;
        gainSum += gain;

        if (successCount == 1) {
            gainMin = gain;
            gainMax = gain;
        } else {
            gainMin = Math.min(gainMin, gain);
            gainMax = Math.max(gainMax, gain);
        }

        var delta = gain - gainMean;
        gainMean += delta / successCount;
        var delta2 = gain - gainMean;
        m2 += delta * delta2;
    }

    public double getGainVariance() {
        if (successCount < 2) {
            return 0.0;
        }
        return m2 / successCount;
    }

    public void merge(MoveStatistics other) {
        this.attemptCount += other.attemptCount;
        this.totalTimeNanos += other.totalTimeNanos;
        this.gainSum += other.gainSum;

        if (other.successCount == 0) {
            return;
        }

        int combinedCount = this.successCount + other.successCount;
        double delta = other.gainMean - this.gainMean;
        double combinedMean =
                (this.successCount * this.gainMean + other.successCount * other.gainMean)
                        / combinedCount;
        double combinedM2 =
                this.m2
                        + other.m2
                        + delta
                                * delta
                                * ((double) this.successCount * other.successCount / combinedCount);

        if (this.successCount == 0) {
            this.gainMin = other.gainMin;
            this.gainMax = other.gainMax;
        } else {
            this.gainMin = Math.min(this.gainMin, other.gainMin);
            this.gainMax = Math.max(this.gainMax, other.gainMax);
        }

        this.gainMean = combinedMean;
        this.m2 = combinedM2;
        this.successCount = combinedCount;
    }
}
