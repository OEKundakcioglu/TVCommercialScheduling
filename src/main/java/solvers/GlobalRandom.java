package solvers;

import java.util.Random;

public class GlobalRandom {
    private static Random random;
    private static Long seed;

    @SuppressWarnings("unused")
    public static void init(Long initialSeed) {
        seed = initialSeed;
        random = new Random(seed);
    }

    public static void init() {
        seed = System.currentTimeMillis();
        random = new Random(seed);
    }

    public static void close() {
        random = null;
        seed = null;
    }

    public static Random getRandom() {
        return random;
    }

    public static Long getSeed() {
        if (seed == null) {
            throw new IllegalStateException("GlobalRandom has not been initialized. Call init() first.");
        }
        return seed;
    }
}

