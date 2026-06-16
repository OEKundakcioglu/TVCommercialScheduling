package scheduling.solver.heuristic.grasp.vnd.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;

class AdaptiveSelectorTest {

    @Test
    void stringDescReturnsFormattedString() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var neighborhoods = List.of(n1);
        var selector = new AdaptiveSelector(0.05, neighborhoods);
        assertEquals("ADAPTIVE(min=0.05)", selector.stringDesc());
    }

    @Test
    void initialSelectionIsUniform() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var n3 = stubNeighborhood(NeighborhoodType.TRANSFER);
        var neighborhoods = List.of(n1, n2, n3);
        var selector = new AdaptiveSelector(0.05, neighborhoods);
        var random = new Random(42);

        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 3000; i++) {
            counts.merge(selector.select(neighborhoods, random), 1, Integer::sum);
        }

        for (var n : neighborhoods) {
            var count = counts.getOrDefault(n, 0);
            assertTrue(count > 800, "Expected roughly uniform: " + count);
            assertTrue(count < 1200, "Expected roughly uniform: " + count);
        }
    }

    @Test
    void biasesTowardHighGainNeighborhood() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var neighborhoods = List.of(n1, n2);
        var selector = new AdaptiveSelector(0.05, neighborhoods);

        // Report n1 as highly productive, n2 as not
        for (int i = 0; i < 10; i++) {
            selector.reportResult(n1, 100.0);
            selector.reportResult(n2, 0.0);
        }
        selector.update();

        var random = new Random(42);
        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 1000; i++) {
            counts.merge(selector.select(neighborhoods, random), 1, Integer::sum);
        }

        assertTrue(
                counts.getOrDefault(n1, 0) > counts.getOrDefault(n2, 0),
                "n1 should be selected more often than n2");
    }

    @Test
    void minProbabilityPreventsStarvation() {
        var minProb = 0.1;
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var neighborhoods = List.of(n1, n2);
        var selector = new AdaptiveSelector(minProb, neighborhoods);

        // Make n1 extremely dominant
        for (int i = 0; i < 100; i++) {
            selector.reportResult(n1, 1000.0);
            selector.reportResult(n2, 0.0);
        }
        selector.update();

        var random = new Random(42);
        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 10000; i++) {
            counts.merge(selector.select(neighborhoods, random), 1, Integer::sum);
        }

        // n2 should still get at least minProbability share
        var n2Count = counts.getOrDefault(n2, 0);
        assertTrue(n2Count >= 500, "n2 should get at least ~10% selections: " + n2Count);
    }

    @Test
    void updateAlwaysRecalculates() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var neighborhoods = List.of(n1, n2);
        var selector = new AdaptiveSelector(0.05, neighborhoods);

        selector.reportResult(n1, 100.0);
        selector.reportResult(n2, 0.0);
        selector.update();

        var random = new Random(42);
        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 2000; i++) {
            counts.merge(selector.select(neighborhoods, random), 1, Integer::sum);
        }
        assertTrue(
                counts.getOrDefault(n1, 0) > counts.getOrDefault(n2, 0),
                "n1 should be favored after single update");
    }

    @Test
    void freshInstanceReturnsIndependentCopy() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var neighborhoods = List.of(n1, n2);
        var selector = new AdaptiveSelector(0.05, neighborhoods);

        selector.reportResult(n1, 100.0);
        selector.reportResult(n2, 0.0);
        selector.update();

        var fresh = selector.freshInstance();

        var random = new Random(42);
        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 3000; i++) {
            counts.merge(fresh.select(neighborhoods, random), 1, Integer::sum);
        }

        for (var n : neighborhoods) {
            var count = counts.getOrDefault(n, 0);
            assertTrue(count > 800, "Fresh instance should be uniform: " + count);
            assertTrue(count < 1700, "Fresh instance should be uniform: " + count);
        }
    }

    @Test
    void gainsAccumulateAcrossUpdates() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var neighborhoods = List.of(n1, n2);
        var selector = new AdaptiveSelector(0.05, neighborhoods);

        // First round: n1 dominant (cumulative: n1=100, n2=0)
        selector.reportResult(n1, 100.0);
        selector.reportResult(n2, 0.0);
        selector.update();

        // Second round: n2 gets more but n1 still leads (cumulative: n1=100, n2=50)
        selector.reportResult(n2, 50.0);
        selector.update();

        // n1 should still be favored due to accumulated advantage
        var random = new Random(42);
        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 2000; i++) {
            counts.merge(selector.select(neighborhoods, random), 1, Integer::sum);
        }
        assertTrue(
                counts.getOrDefault(n1, 0) > counts.getOrDefault(n2, 0),
                "n1 should still be favored due to accumulated gains");
    }

    @Test
    void noRecalculationWhenAllGainsEqual() {
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.SHIFT);
        var neighborhoods = List.of(n1, n2);
        var selector = new AdaptiveSelector(0.05, neighborhoods);

        // Equal gains — shifted total is zero, probabilities stay uniform
        selector.reportResult(n1, 50.0);
        selector.reportResult(n2, 50.0);
        selector.update();

        var random = new Random(42);
        var counts = new HashMap<Neighborhood, Integer>();
        for (int i = 0; i < 2000; i++) {
            counts.merge(selector.select(neighborhoods, random), 1, Integer::sum);
        }
        var n1Count = counts.getOrDefault(n1, 0);
        assertTrue(
                n1Count > 800 && n1Count < 1200,
                "Should remain uniform when gains are equal: " + n1Count);
    }

    private Neighborhood stubNeighborhood(NeighborhoodType type) {
        return new Neighborhood() {
            @Override
            public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
                return List.of();
            }

            @Override
            public NeighborhoodType type() {
                return type;
            }
        };
    }
}
