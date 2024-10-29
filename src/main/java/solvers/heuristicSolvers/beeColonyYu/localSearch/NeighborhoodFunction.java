package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

import java.util.List;
import java.util.Random;

public class NeighborhoodFunction {

    private List<IMove> moves;
    private BeeColonyUtils beeColonyUtils;

    public NeighborhoodFunction(BeeColonyUtils beeColonyUtils, Random random) {
        this.beeColonyUtils = beeColonyUtils;
        this.moves =
                List.of(
                        new InsertMove(beeColonyUtils, random),
                        new SwapMove(beeColonyUtils, random),
                        new InversionMove(beeColonyUtils, random));
    }

    public BeeColonySolution apply(BeeColonySolution solution) throws Exception {
        var index = new Random().nextInt(moves.size());
        return moves.get(index).apply(solution);
    }
}
