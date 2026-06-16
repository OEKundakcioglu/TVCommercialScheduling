package scheduling.solver.heuristic.grasp.vnd.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;

class SequentialSelectorTest {

    @Test
    void cyclesThroughNeighborhoodsInOrder() {
        var selector = new SequentialSelector();
        var n1 = stubNeighborhood(NeighborhoodType.INSERT);
        var n2 = stubNeighborhood(NeighborhoodType.INSERT);
        var n3 = stubNeighborhood(NeighborhoodType.INSERT);
        var neighborhoods = List.of(n1, n2, n3);
        var random = new Random(42);

        assertEquals(n1, selector.select(neighborhoods, random));
        assertEquals(n2, selector.select(neighborhoods, random));
        assertEquals(n3, selector.select(neighborhoods, random));
        assertEquals(n1, selector.select(neighborhoods, random));
    }

    @Test
    void stringDescReturnsSEQ() {
        var selector = new SequentialSelector();
        assertEquals("SEQ", selector.stringDesc());
    }

    @Test
    void reportResultIsNoOp() {
        var selector = new SequentialSelector();
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
