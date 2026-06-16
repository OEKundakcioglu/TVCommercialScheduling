package scheduling.solver.heuristic.beecolony.move;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NeighborhoodFunctionTest {

    @Test
    void appliesAMoveAndPreservesElements() {
        var nf = new NeighborhoodFunction();
        var input = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
        for (int seed = 0; seed < 100; seed++) {
            var result = nf.apply(input, new Random(seed));
            assertEquals(input.length, result.length, "Seed " + seed);
            var sortedInput = input.clone();
            var sortedResult = result.clone();
            Arrays.sort(sortedInput);
            Arrays.sort(sortedResult);
            assertArrayEquals(sortedInput, sortedResult, "Seed " + seed);
        }
    }

    @Test
    void usesAllThreeMoveTypes() {
        var nf = new NeighborhoodFunction();
        var input = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        var diffCounts = new HashSet<Integer>();

        for (int seed = 0; seed < 100; seed++) {
            var result = nf.apply(input, new Random(seed));
            var diffCount = 0;
            for (int i = 0; i < input.length; i++) {
                if (input[i] != result[i]) {
                    diffCount++;
                }
            }
            diffCounts.add(diffCount);
        }

        assertTrue(
                diffCounts.size() >= 3,
                "Expected at least 3 distinct diff-count patterns across 100 seeds, got "
                        + diffCounts);
    }
}
