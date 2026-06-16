package scheduling.solver.heuristic.beecolony.move;

import java.util.Random;

public class InversionMove implements BeeColonyMove {

    @Override
    public int[] apply(int[] solutionString, Random random) {
        var low = random.nextInt(solutionString.length - 1);
        var high = random.nextInt(low + 1, solutionString.length);
        var result = solutionString.clone();

        for (int i = low, j = high; i < j; i++, j--) {
            var temp = result[i];
            result[i] = result[j];
            result[j] = temp;
        }

        return result;
    }
}
