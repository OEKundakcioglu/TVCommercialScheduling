package scheduling.solver.heuristic.beecolony.move;

import java.util.Random;

public interface BeeColonyMove {
    int[] apply(int[] solutionString, Random random);
}
