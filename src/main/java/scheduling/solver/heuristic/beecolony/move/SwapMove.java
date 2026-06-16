package scheduling.solver.heuristic.beecolony.move;

import java.util.Random;

public class SwapMove implements BeeColonyMove {

    @Override
    public int[] apply(int[] solutionString, Random random) {
        var indices = random.ints(0, solutionString.length).distinct().limit(2).toArray();
        var i = indices[0];
        var j = indices[1];
        var result = solutionString.clone();

        result[i] = solutionString[j];
        result[j] = solutionString[i];

        return result;
    }
}
