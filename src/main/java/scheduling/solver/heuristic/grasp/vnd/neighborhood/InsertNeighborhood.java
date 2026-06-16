package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.InsertMove;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.MoveIterator;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator.NestedRandomIterator;

@RequiredArgsConstructor
public class InsertNeighborhood implements Neighborhood {

    private final Problem problem;

    @Override
    public Iterable<Move> generateMoves(GraspSolution solution, Random random) {
        var unassigned = Neighborhoods.findUnassignedCommercials(problem, solution);
        IntFunction<int[]> invFactory = problem::getSuitableInventories;
        IntFunction<int[]> posFactory =
                invId -> IntStream.rangeClosed(0, solution.getSequences()[invId].length).toArray();
        var tuples = new NestedRandomIterator(unassigned, List.of(invFactory, posFactory), random);
        return () ->
                new MoveIterator(
                        tuples,
                        tuple -> new InsertMove(problem, solution, tuple[1], tuple[2], tuple[0]));
    }

    @Override
    public NeighborhoodType type() {
        return NeighborhoodType.INSERT;
    }
}
