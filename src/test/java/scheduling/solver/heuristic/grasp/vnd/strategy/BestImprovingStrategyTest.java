package scheduling.solver.heuristic.grasp.vnd.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.move.Move;

class BestImprovingStrategyTest {

    @Test
    void returnsEmptyWhenNoCandidates() {
        var strategy = new BestImprovingStrategy();

        var result = strategy.selectMove(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyWhenAllNonImproving() {
        var strategy = new BestImprovingStrategy();
        var moves = List.<Move>of(new StubMove(true, 0.0), new StubMove(true, -5.0));

        var result = strategy.selectMove(moves);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsBestFeasibleImprovingMove() {
        var strategy = new BestImprovingStrategy();
        var move1 = new StubMove(true, 10.0);
        var move2 = new StubMove(true, 30.0);
        var move3 = new StubMove(true, 20.0);

        var result = strategy.selectMove(List.of(move1, move2, move3));

        assertTrue(result.isPresent());
        assertEquals(move2, result.get());
    }

    @Test
    void skipsInfeasibleMoves() {
        var strategy = new BestImprovingStrategy();
        var move1 = new StubMove(false, 100.0);
        var move2 = new StubMove(true, 10.0);

        var result = strategy.selectMove(List.of(move1, move2));

        assertTrue(result.isPresent());
        assertEquals(move2, result.get());
    }

    @Test
    void picksBestAmongFeasible() {
        var strategy = new BestImprovingStrategy();
        var move1 = new StubMove(true, 5.0);
        var move2 = new StubMove(false, 100.0);
        var move3 = new StubMove(true, 15.0);
        var move4 = new StubMove(true, 10.0);

        var result = strategy.selectMove(List.of(move1, move2, move3, move4));

        assertTrue(result.isPresent());
        assertEquals(move3, result.get());
    }

    @Test
    void stringDescReturnsBest() {
        var strategy = new BestImprovingStrategy();
        assertEquals("BEST", strategy.stringDesc());
    }
}
