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
import scheduling.solver.heuristic.grasp.move.TransferMove;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.MoveIterator;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.NestedRandomIterator;

@RequiredArgsConstructor
public class TransferNeighborhood implements Neighborhood {

    private final Problem problem;

    @Override
    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
        var nonEmptyInvs = findNonEmptyInventories(solution);
        var currentSourceInv = new int[1];
        IntFunction<int[]> fromPosFactory =
                sourceInv -> {
                    currentSourceInv[0] = sourceInv;
                    return IntStream.range(0, solution.getSequences()[sourceInv].length).toArray();
                };
        IntFunction<int[]> destInvFactory =
                fromPos -> {
                    var commId = solution.getSequences()[currentSourceInv[0]][fromPos];
                    return findSuitableDestinations(commId, currentSourceInv[0]);
                };
        IntFunction<int[]> toPosFactory =
                destInv ->
                        IntStream.rangeClosed(0, solution.getSequences()[destInv].length).toArray();
        var tuples =
                new NestedRandomIterator(
                        nonEmptyInvs,
                        List.of(fromPosFactory, destInvFactory, toPosFactory),
                        random);
        return () ->
                new MoveIterator(
                        tuples,
                        tuple ->
                                new TransferMove(
                                        problem, solution, tuple[0], tuple[1], tuple[2], tuple[3]));
    }

    @Override
    public NeighborhoodType type() {
        return NeighborhoodType.TRANSFER;
    }

    private int[] findNonEmptyInventories(GraspSolution solution) {
        return IntStream.range(0, problem.getInventories().length)
                .filter(invId -> solution.getSequences()[invId].length > 0)
                .toArray();
    }

    private int[] findSuitableDestinations(int commId, int sourceInvId) {
        return Arrays.stream(problem.getSuitableInventories(commId))
                .filter(invId -> invId != sourceInvId)
                .toArray();
    }
}
