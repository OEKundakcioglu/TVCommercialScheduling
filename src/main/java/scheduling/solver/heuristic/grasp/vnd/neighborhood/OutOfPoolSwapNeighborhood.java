package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.move.OutOfPoolSwapMove;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.MoveIterator;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.NestedRandomIterator;

@RequiredArgsConstructor
public class OutOfPoolSwapNeighborhood implements Neighborhood {

    private final Problem problem;

    @Override
    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
        var unassigned = Neighborhoods.findUnassignedCommercials(problem, solution);
        IntFunction<int[]> invFactory = commId -> findNonEmptySuitableInventories(commId, solution);
        IntFunction<int[]> posFactory =
                invId -> IntStream.range(0, solution.getSequences()[invId].length).toArray();
        var tuples = new NestedRandomIterator(unassigned, List.of(invFactory, posFactory), random);
        return () ->
                new MoveIterator(
                        tuples,
                        tuple ->
                                new OutOfPoolSwapMove(
                                        problem, solution, tuple[1], tuple[2], tuple[0]));
    }

    @Override
    public NeighborhoodType type() {
        return NeighborhoodType.OUT_OF_POOL_SWAP;
    }

    private int[] findNonEmptySuitableInventories(int commId, GraspSolution solution) {
        return Arrays.stream(problem.getSuitableInventories(commId))
                .filter(invId -> solution.getSequences()[invId].length > 0)
                .toArray();
    }
}
