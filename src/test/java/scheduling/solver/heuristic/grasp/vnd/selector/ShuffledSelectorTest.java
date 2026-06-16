package scheduling.solver.heuristic.grasp.vnd.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;

class ShuffledSelectorTest {

    @Test
    void selectsFromAllNeighborhoods() {
        var selector = new ShuffledSelector();
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.INSERT);
        var n3 = stubNeighborhood(NeighborhoodType.INSERT);
        var neighborhoods = List.of(n1, n2, n3);

        var random = new Random(42);
        var selected = new HashSet<Neighborhood>();
        for (int i = 0; i < 100; i++) {
            selected.add(selector.select(neighborhoods, random));
        }

        assertTrue(selected.contains(n1));
        assertTrue(selected.contains(n2));
        assertTrue(selected.contains(n3));
    }

    @Test
    void stringDescReturnsShuf() {
        var selector = new ShuffledSelector();
        assertEquals("SHUF", selector.stringDesc());
    }

    @Test
    void reportResultIsNoOp() {
        var selector = new ShuffledSelector();
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);

        selector.reportResult(n1, 100.0);

        assertEquals(n1, selector.select(List.of(n1), new Random(42)));
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
