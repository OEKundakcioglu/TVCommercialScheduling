package scheduling.solver.heuristic.grasp.vnd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;
import scheduling.solver.heuristic.grasp.vnd.strategy.StubMove;

class VNDTest {

    @Test
    void returnsInitialSolutionWhenNoImprovingMoves() {
        var initial = buildEmptySolution();
        var neighborhood = emptyNeighborhood(NeighborhoodType.INSERT);
        var config =
                new VNDConfig(
                        new FirstImprovingStrategy(),
                        List.of(neighborhood),
                        new SequentialSelector(),
                        0.0);
        var ls = new VND(config, new java.util.Random(42));

        var result = ls.search(initial);

        assertSame(initial, result);
    }

    @Test
    void stopsWhenAllNeighborhoodsExhausted() {
        var initial = buildEmptySolution();
        var n1 = emptyNeighborhood(NeighborhoodType.INSERT);
        var n2 = emptyNeighborhood(NeighborhoodType.INSERT);
        var n3 = emptyNeighborhood(NeighborhoodType.INSERT);
        var config =
                new VNDConfig(
                        new FirstImprovingStrategy(),
                        List.of(n1, n2, n3),
                        new SequentialSelector(),
                        0.0);
        var ls = new VND(config, new java.util.Random(42));

        ls.search(initial);

        assertEquals(3, ls.getStatistics().getTotalIterations());
        assertEquals(0, ls.getStatistics().getTotalImprovements());
    }

    @Test
    void appliesImprovingMoveAndResetsCounter() {
        var solution1 = buildEmptySolution();
        var solution2 = buildEmptySolution();

        var improvingMove =
                new StubMove(true, 10.0) {
                    @Override
                    public GraspSolution apply() {
                        return solution2;
                    }
                };

        var callCount = new int[] {0};
        Neighborhood neighborhood =
                new Neighborhood() {
                    @Override
                    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
                        callCount[0]++;
                        if (callCount[0] == 1) {
                            return List.of(improvingMove);
                        }
                        return List.of();
                    }

                    @Override
                    public NeighborhoodType type() {
                        return NeighborhoodType.INSERT;
                    }
                };

        var config =
                new VNDConfig(
                        new FirstImprovingStrategy(),
                        List.of(neighborhood),
                        new SequentialSelector(),
                        0.0);
        var ls = new VND(config, new java.util.Random(42));

        var result = ls.search(solution1);

        assertSame(solution2, result);
        assertEquals(2, ls.getStatistics().getTotalIterations());
        assertEquals(1, ls.getStatistics().getTotalImprovements());
    }

    @Test
    void recordsMoveStatisticsPerNeighborhood() {
        var initial = buildEmptySolution();
        var n1 = emptyNeighborhood(NeighborhoodType.INSERT);
        var config =
                new VNDConfig(
                        new FirstImprovingStrategy(), List.of(n1), new SequentialSelector(), 0.0);
        var ls = new VND(config, new java.util.Random(42));

        ls.search(initial);

        var moveStats = ls.getStatistics().getMoveStatistics();
        assertEquals(1, moveStats.size());
        assertTrue(moveStats.containsKey(NeighborhoodType.INSERT));
    }

    private GraspSolution buildEmptySolution() {
        return new GraspSolution(
                new int[0][],
                new int[0][],
                new double[0][],
                0.0,
                new int[0],
                new int[0],
                new int[0],
                new int[0]);
    }

    private Neighborhood emptyNeighborhood(NeighborhoodType type) {
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
