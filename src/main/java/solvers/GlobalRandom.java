package solvers;

import java.util.Random;

public class GlobalRandom {
    private static Random random = new Random();

    public static void setSeed(int seed) {
        random = new Random(seed);
    }

    public static Random getRandom() {
        return random;
    }
}
