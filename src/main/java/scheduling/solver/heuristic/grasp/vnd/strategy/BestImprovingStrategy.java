package scheduling.solver.heuristic.grasp.vnd.strategy;

import java.util.Optional;
import scheduling.solver.heuristic.grasp.move.Move;

public class BestImprovingStrategy implements SearchStrategy {

    @Override
    public String stringDesc() {
        return "BEST";
    }

    @Override
    public Optional<Move> selectMove(Iterable<Move> candidates) {
        Move bestMove = null;
        var bestGain = 0.0;

        for (Move move : candidates) {
            if (!move.checkFeasibility()) {
                continue;
            }
            var gain = move.calculateRevenueGain();
            if (gain > bestGain) {
                bestGain = gain;
                bestMove = move;
            }
        }
        return Optional.ofNullable(bestMove);
    }
}
