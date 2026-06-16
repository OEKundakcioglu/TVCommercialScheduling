package scheduling.solver.heuristic.grasp.vnd.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;

class SearchStatisticsTest {

    @Test
    void initialValuesAreZero() {
        var stats = new SearchStatistics();

        assertEquals(0, stats.getTotalIterations());
        assertEquals(0, stats.getTotalImprovements());
        assertTrue(stats.getMoveStatistics().isEmpty());
    }

    @Test
    void getOrCreateMoveStatisticsReturnsSameInstance() {
        var stats = new SearchStatistics();

        var insert1 = stats.getOrCreateMoveStatistics(NeighborhoodType.INSERT);
        var insert2 = stats.getOrCreateMoveStatistics(NeighborhoodType.INSERT);

        assertSame(insert1, insert2);
    }

    @Test
    void getOrCreateMoveStatisticsCreatesDistinctPerType() {
        var stats = new SearchStatistics();

        var insert = stats.getOrCreateMoveStatistics(NeighborhoodType.INSERT);

        assertNotNull(insert);
        assertEquals(1, stats.getMoveStatistics().size());
    }

    @Test
    void recordsIteration() {
        var stats = new SearchStatistics();

        stats.recordIteration();
        stats.recordIteration();
        stats.recordIteration();

        assertEquals(3, stats.getTotalIterations());
    }

    @Test
    void recordsImprovement() {
        var stats = new SearchStatistics();

        stats.recordImprovement();
        stats.recordImprovement();

        assertEquals(2, stats.getTotalImprovements());
    }
}
