package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

import java.util.List;
import java.util.Random;

public class NeighborhoodFunction {

    private final List<IMove> moves;

    public NeighborhoodFunction(BeeColonyUtils beeColonyUtils) {
        this.moves =
                List.of(
                        new InsertMove(beeColonyUtils),
                        new SwapMove(beeColonyUtils),
                        new InversionMove(beeColonyUtils));
    }

    public BeeColonySolution apply(BeeColonySolution solution) {
        var index = new Random().nextInt(moves.size());
        return moves.get(index).apply(solution);
    }
}
