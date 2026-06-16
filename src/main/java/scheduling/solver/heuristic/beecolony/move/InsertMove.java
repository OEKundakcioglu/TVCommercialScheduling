package scheduling.solver.heuristic.beecolony.move;

import java.util.Random;

public class InsertMove implements BeeColonyMove {

    @Override
    public int[] apply(int[] solutionString, Random random) {
        var indices = random.ints(0, solutionString.length).distinct().limit(2).toArray();
        var from = indices[0];
        var to = indices[1];
        var element = solutionString[from];
        var result = new int[solutionString.length];

        if (from < to) {
            System.arraycopy(solutionString, 0, result, 0, from);
            System.arraycopy(solutionString, from + 1, result, from, to - from);
            result[to] = element;
            System.arraycopy(
                    solutionString, to + 1, result, to + 1, solutionString.length - to - 1);
        } else {
            System.arraycopy(solutionString, 0, result, 0, to);
            result[to] = element;
            System.arraycopy(solutionString, to, result, to + 1, from - to);
            System.arraycopy(
                    solutionString, from + 1, result, from + 1, solutionString.length - from - 1);
        }

        return result;
    }
}
