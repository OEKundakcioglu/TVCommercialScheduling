package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.InterSwapMove;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.MoveIterator;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.NestedRandomIterator;

@RequiredArgsConstructor
public class InterSwapNeighborhood implements Neighborhood {

    private final Problem problem;

    @Override
    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
        var nonEmptyInvs = findNonEmptyInventories(solution);
        var currentInv1 = new int[1];
        var currentInv2 = new int[1];
        IntFunction<int[]> inv2Factory =
                inv1 -> {
                    currentInv1[0] = inv1;
                    return copyWithout(nonEmptyInvs, inv1);
                };
        IntFunction<int[]> pos1Factory =
                inv2 -> {
                    currentInv2[0] = inv2;
                    return IntStream.range(0, solution.getSequences()[currentInv1[0]].length)
                            .toArray();
                };
        IntFunction<int[]> pos2Factory =
                _ -> IntStream.range(0, solution.getSequences()[currentInv2[0]].length).toArray();
        var tuples =
                new NestedRandomIterator(
                        nonEmptyInvs, List.of(inv2Factory, pos1Factory, pos2Factory), random);
        return () ->
                new MoveIterator(
                        tuples,
                        tuple ->
                                new InterSwapMove(
                                        problem, solution, tuple[0], tuple[2], tuple[1], tuple[3]));
    }

    @Override
    public NeighborhoodType type() {
        return NeighborhoodType.INTER_SWAP;
    }

    private int[] findNonEmptyInventories(GraspSolution solution) {
        return IntStream.range(0, problem.getInventories().length)
                .filter(invId -> solution.getSequences()[invId].length > 0)
                .toArray();
    }

    private int[] copyWithout(int[] array, int exclude) {
        var result = new int[array.length - 1];
        var idx = 0;
        for (var value : array) {
            if (value != exclude) {
                result[idx++] = value;
            }
        }
        return result;
    }
}
