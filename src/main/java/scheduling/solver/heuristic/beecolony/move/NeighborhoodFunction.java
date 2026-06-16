package scheduling.solver.heuristic.beecolony.move;

import java.util.List;
import java.util.Random;

public class NeighborhoodFunction {

    private final List<BeeColonyMove> moves =
            List.of(new InsertMove(), new SwapMove(), new InversionMove());

    public int[] apply(int[] solutionString, Random random) {
        var index = random.nextInt(moves.size());
        return moves.get(index).apply(solutionString, random);
    }
}
