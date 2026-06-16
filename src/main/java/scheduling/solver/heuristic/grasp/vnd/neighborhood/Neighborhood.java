package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.Random;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.Move;

public interface Neighborhood {

    Iterable<Move> generateMoves(GraspSolution solution, Random random);

    NeighborhoodType type();
}
