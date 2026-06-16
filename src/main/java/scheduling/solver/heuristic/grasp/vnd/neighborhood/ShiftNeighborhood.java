package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.move.ShiftMove;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.MoveIterator;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.NestedRandomIterator;

@RequiredArgsConstructor
public class ShiftNeighborhood implements Neighborhood {

    private final Problem problem;

    @Override
    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
        var shiftableInvs = findShiftableInventories(solution);
        var currentInv = new int[1];
        IntFunction<int[]> fromPosFactory =
                invId -> {
                    currentInv[0] = invId;
                    return IntStream.range(0, solution.getSequences()[invId].length).toArray();
                };
        IntFunction<int[]> toPosFactory =
                fromPos ->
                        IntStream.range(0, solution.getSequences()[currentInv[0]].length)
                                .filter(p -> p != fromPos)
                                .toArray();
        var tuples =
                new NestedRandomIterator(
                        shiftableInvs, List.of(fromPosFactory, toPosFactory), random);
        return () ->
                new MoveIterator(
                        tuples,
                        tuple -> new ShiftMove(problem, solution, tuple[0], tuple[1], tuple[2]));
    }

    @Override
    public NeighborhoodType type() {
        return NeighborhoodType.SHIFT;
    }

    private int[] findShiftableInventories(GraspSolution solution) {
        return IntStream.range(0, problem.getInventories().length)
                .filter(invId -> solution.getSequences()[invId].length >= 2)
                .toArray();
    }
}
