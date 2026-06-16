package scheduling.solver.heuristic.grasp.vnd.strategy;

import java.util.Optional;
import scheduling.solver.heuristic.grasp.move.Move;

public interface SearchStrategy {

    Optional<Move> selectMove(Iterable<Move> candidates);

    String stringDesc();
}
