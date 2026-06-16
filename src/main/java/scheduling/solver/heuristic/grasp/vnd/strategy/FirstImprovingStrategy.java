package scheduling.solver.heuristic.grasp.vnd.strategy;

import java.util.Optional;
import scheduling.solver.heuristic.grasp.move.Move;

public class FirstImprovingStrategy implements SearchStrategy {

    @Override
    public String stringDesc() {
        return "FIRST";
    }

    @Override
    public Optional<Move> selectMove(Iterable<Move> candidates) {
        for (Move move : candidates) {
            if (!move.checkFeasibility()) {
                continue;
            }
            if (move.calculateRevenueGain() > 0) {
                return Optional.of(move);
            }
        }
        return Optional.empty();
    }
}
