package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.IntraSwapMove;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.MoveIterator;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.NestedRandomIterator;

@RequiredArgsConstructor
public class IntraSwapNeighborhood implements Neighborhood {

    private final Problem problem;

    @Override
    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
        var swappableInvs = findSwappableInventories(solution);
        var currentInv = new int[1];
        IntFunction<int[]> pos1Factory =
                invId -> {
                    currentInv[0] = invId;
                    return IntStream.range(0, solution.getSequences()[invId].length - 1).toArray();
                };
        IntFunction<int[]> pos2Factory =
                pos1 ->
                        IntStream.range(pos1 + 1, solution.getSequences()[currentInv[0]].length)
                                .toArray();
        var tuples =
                new NestedRandomIterator(swappableInvs, List.of(pos1Factory, pos2Factory), random);
        return () ->
                new MoveIterator(
                        tuples,
                        tuple ->
                                new IntraSwapMove(problem, solution, tuple[0], tuple[1], tuple[2]));
    }

    @Override
    public NeighborhoodType type() {
        return NeighborhoodType.INTRA_SWAP;
    }

    private int[] findSwappableInventories(GraspSolution solution) {
        return IntStream.range(0, problem.getInventories().length)
                .filter(invId -> solution.getSequences()[invId].length >= 2)
                .toArray();
    }
}
