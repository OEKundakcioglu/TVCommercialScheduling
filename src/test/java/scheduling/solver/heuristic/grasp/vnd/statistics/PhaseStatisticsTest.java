package scheduling.solver.heuristic.grasp.vnd.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PhaseStatisticsTest {

    @Test
    void initialValuesAreZero() {
        var stats = new PhaseStatistics();

        assertEquals(0, stats.getCallCount());
        assertEquals(0L, stats.getTotalTimeNanos());
        assertEquals(0.0, stats.getTotalRevenueGain(), 1e-9);
    }

    @Test
    void recordsSinglePhase() {
        var stats = new PhaseStatistics();

        stats.record(500L, 100.0);

        assertEquals(1, stats.getCallCount());
        assertEquals(500L, stats.getTotalTimeNanos());
        assertEquals(100.0, stats.getTotalRevenueGain(), 1e-9);
    }

    @Test
    void accumulatesMultiplePhases() {
        var stats = new PhaseStatistics();

        stats.record(500L, 100.0);
        stats.record(300L, -20.0);
        stats.record(200L, 50.0);

        assertEquals(3, stats.getCallCount());
        assertEquals(1000L, stats.getTotalTimeNanos());
        assertEquals(130.0, stats.getTotalRevenueGain(), 1e-9);
    }
}
