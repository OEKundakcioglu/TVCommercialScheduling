package scheduling.solver.heuristic.grasp.elitepool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.GraspSolution;

class ElitePoolTest {

    private static final int NUM_COMMERCIALS = 3;

    private GraspSolution buildSolution(double revenue, int[] assignedInvId) {
        return new GraspSolution(
                new int[0][],
                new int[0][],
                new double[0][],
                revenue,
                new int[0],
                new int[0],
                assignedInvId,
                new int[assignedInvId.length]);
    }

    @Test
    void add_addsWhenPoolNotFull() {
        var pool = new ElitePool(3, NUM_COMMERCIALS);
        var solution = buildSolution(100.0, new int[] {0, 1, 2});

        pool.add(solution);

        assertEquals(1, pool.size());
    }

    @Test
    void add_rejectsDuplicateSolution() {
        var pool = new ElitePool(3, NUM_COMMERCIALS);
        var s1 = buildSolution(100.0, new int[] {0, 1, 2});
        var s2 = buildSolution(200.0, new int[] {0, 1, 2});

        pool.add(s1);
        pool.add(s2);

        assertEquals(1, pool.size());
    }

    @Test
    void add_addsDiverseSolution() {
        var pool = new ElitePool(3, NUM_COMMERCIALS);
        var s1 = buildSolution(100.0, new int[] {0, 1, 2});
        var s2 = buildSolution(200.0, new int[] {1, 0, 2});

        pool.add(s1);
        pool.add(s2);

        assertEquals(2, pool.size());
    }

    @Test
    void add_replacesWorstWhenFullAndCandidateIsBetterAndDiverse() {
        var pool = new ElitePool(2, NUM_COMMERCIALS);
        var s1 = buildSolution(100.0, new int[] {0, 1, 2});
        var s2 = buildSolution(200.0, new int[] {1, 0, 2});
        var s3 = buildSolution(150.0, new int[] {2, 1, 0});

        pool.add(s1);
        pool.add(s2);
        pool.add(s3);

        assertEquals(2, pool.size());
        var random = new Random(42);
        var found100 = false;
        for (int i = 0; i < 100; i++) {
            var guide = pool.getRandomGuide(random);
            if (guide.getTotalRevenue() == 100.0) {
                found100 = true;
            }
        }
        assertEquals(false, found100, "Worst solution (100.0) should have been replaced");
    }

    @Test
    void add_doesNotReplaceWhenFullAndCandidateIsWorse() {
        var pool = new ElitePool(2, NUM_COMMERCIALS);
        var s1 = buildSolution(200.0, new int[] {0, 1, 2});
        var s2 = buildSolution(300.0, new int[] {1, 0, 2});
        var s3 = buildSolution(100.0, new int[] {2, 1, 0});

        pool.add(s1);
        pool.add(s2);
        pool.add(s3);

        assertEquals(2, pool.size());
        var random = new Random(42);
        var found100 = false;
        for (int i = 0; i < 100; i++) {
            var guide = pool.getRandomGuide(random);
            if (guide.getTotalRevenue() == 100.0) {
                found100 = true;
            }
        }
        assertEquals(false, found100, "Worse candidate (100.0) should not be in the pool");
    }

    @Test
    void getRandomGuide_returnsSolution() {
        var pool = new ElitePool(3, NUM_COMMERCIALS);
        var s1 = buildSolution(100.0, new int[] {0, 1, 2});
        pool.add(s1);

        var guide = pool.getRandomGuide(new Random(42));

        assertNotNull(guide);
        assertSame(s1, guide);
    }

    @Test
    void threadSafe_basicFunctionalTest() {
        var pool = ElitePool.threadSafe(3, NUM_COMMERCIALS);
        var s1 = buildSolution(100.0, new int[] {0, 1, 2});
        var s2 = buildSolution(200.0, new int[] {1, 0, 2});

        pool.add(s1);
        pool.add(s2);

        assertEquals(2, pool.size());
        var guide = pool.getRandomGuide(new Random(42));
        assertNotNull(guide);
    }
}
