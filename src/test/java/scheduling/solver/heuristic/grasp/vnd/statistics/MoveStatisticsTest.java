package scheduling.solver.heuristic.grasp.vnd.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MoveStatisticsTest {

    @Test
    void emptyStatisticsHaveZeroValues() {
        var stats = new MoveStatistics();

        assertEquals(0, stats.getAttemptCount());
        assertEquals(0, stats.getSuccessCount());
        assertEquals(0.0, stats.getGainSum(), 1e-9);
        assertEquals(0.0, stats.getGainMin(), 1e-9);
        assertEquals(0.0, stats.getGainMax(), 1e-9);
        assertEquals(0.0, stats.getGainMean(), 1e-9);
        assertEquals(0.0, stats.getGainVariance(), 1e-9);
        assertEquals(0L, stats.getTotalTimeNanos());
    }

    @Test
    void recordsSingleSuccess() {
        var stats = new MoveStatistics();

        stats.recordAttempt(500L);
        stats.recordSuccess(10.0);

        assertEquals(1, stats.getAttemptCount());
        assertEquals(1, stats.getSuccessCount());
        assertEquals(10.0, stats.getGainSum(), 1e-9);
        assertEquals(10.0, stats.getGainMin(), 1e-9);
        assertEquals(10.0, stats.getGainMax(), 1e-9);
        assertEquals(10.0, stats.getGainMean(), 1e-9);
        assertEquals(0.0, stats.getGainVariance(), 1e-9);
        assertEquals(500L, stats.getTotalTimeNanos());
    }

    @Test
    void recordsMultipleSuccessesWithCorrectVariance() {
        var stats = new MoveStatistics();

        stats.recordAttempt(100L);
        stats.recordSuccess(10.0);
        stats.recordAttempt(200L);
        stats.recordSuccess(20.0);
        stats.recordAttempt(300L);
        stats.recordSuccess(30.0);

        assertEquals(3, stats.getAttemptCount());
        assertEquals(3, stats.getSuccessCount());
        assertEquals(60.0, stats.getGainSum(), 1e-9);
        assertEquals(10.0, stats.getGainMin(), 1e-9);
        assertEquals(30.0, stats.getGainMax(), 1e-9);
        assertEquals(20.0, stats.getGainMean(), 1e-9);
        assertEquals(200.0 / 3.0, stats.getGainVariance(), 1e-9);
        assertEquals(600L, stats.getTotalTimeNanos());
    }

    @Test
    void failedAttemptsDoNotAffectGainStats() {
        var stats = new MoveStatistics();

        stats.recordAttempt(100L);
        stats.recordSuccess(10.0);
        stats.recordAttempt(100L); // failed attempt, no recordSuccess

        assertEquals(2, stats.getAttemptCount());
        assertEquals(1, stats.getSuccessCount());
        assertEquals(10.0, stats.getGainMin(), 1e-9);
        assertEquals(10.0, stats.getGainMax(), 1e-9);
    }

    @Test
    void tracksNegativeGains() {
        var stats = new MoveStatistics();

        stats.recordAttempt(100L);
        stats.recordSuccess(-5.0);
        stats.recordAttempt(100L);
        stats.recordSuccess(-15.0);

        assertEquals(-20.0, stats.getGainSum(), 1e-9);
        assertEquals(-15.0, stats.getGainMin(), 1e-9);
        assertEquals(-5.0, stats.getGainMax(), 1e-9);
    }
}
