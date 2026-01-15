package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

import java.util.List;
import java.util.Random;

public class NeighborhoodFunction {

    private final List<IMove> moves;
    private final Random random;

    public NeighborhoodFunction(BeeColonyUtils beeColonyUtils, Random random) {
        this.random = random;
        this.moves =
                List.of(
                        new InsertMove(beeColonyUtils, random),
                        new SwapMove(beeColonyUtils, random),
                        new InversionMove(beeColonyUtils, random));
    }

    public BeeColonySolution apply(BeeColonySolution solution) {
        var index = random.nextInt(moves.size());
        return moves.get(index).apply(solution);
    }
}
