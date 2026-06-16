package scheduling.solver.heuristic.grasp.vnd.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.move.Move;

class FirstImprovingStrategyTest {

    @Test
    void returnsEmptyWhenNoCandidates() {
        var strategy = new FirstImprovingStrategy();

        var result = strategy.selectMove(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenAllInfeasible() {
        var strategy = new FirstImprovingStrategy();
        var moves = List.<Move>of(new StubMove(false, 10.0), new StubMove(false, 20.0));

        var result = strategy.selectMove(moves);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenAllNonImproving() {
        var strategy = new FirstImprovingStrategy();
        var moves = List.<Move>of(new StubMove(true, 0.0), new StubMove(true, -5.0));

        var result = strategy.selectMove(moves);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsFirstFeasibleImprovingMove() {
        var strategy = new FirstImprovingStrategy();
        var move1 = new StubMove(true, -5.0);
        var move2 = new StubMove(true, 10.0);
        var move3 = new StubMove(true, 20.0);

        var result = strategy.selectMove(List.of(move1, move2, move3));

        assertTrue(result.isPresent());
        assertEquals(move2, result.get());
    }

    @Test
    void skipsInfeasibleAndReturnsFirstImproving() {
        var strategy = new FirstImprovingStrategy();
        var move1 = new StubMove(false, 50.0);
        var move2 = new StubMove(true, 10.0);

        var result = strategy.selectMove(List.of(move1, move2));

        assertTrue(result.isPresent());
        assertEquals(move2, result.get());
    }

    @Test
    void stringDescReturnsFirst() {
        var strategy = new FirstImprovingStrategy();
        assertEquals("FIRST", strategy.stringDesc());
    }
}
