package scheduling.solver.heuristic.beecolony.move;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class InversionMoveTest {

    @Test
    void preservesAllElements() {
        var move = new InversionMove();
        var input = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
        for (int seed = 0; seed < 100; seed++) {
            var result = move.apply(input, new Random(seed));
            var sortedInput = input.clone();
            var sortedResult = result.clone();
            Arrays.sort(sortedInput);
            Arrays.sort(sortedResult);
            assertArrayEquals(sortedInput, sortedResult, "Seed " + seed);
        }
    }

    @Test
    void doesNotModifyOriginal() {
        var move = new InversionMove();
        var input = new int[] {5, 3, 1, 4, 2};
        var original = input.clone();
        move.apply(input, new Random(42));
        assertArrayEquals(original, input);
    }

    @Test
    void resultLengthMatchesInput() {
        var move = new InversionMove();
        var input = new int[] {10, 20, 30, 40, 50};
        for (int seed = 0; seed < 50; seed++) {
            var result = move.apply(input, new Random(seed));
            assertEquals(input.length, result.length, "Seed " + seed);
        }
    }

    @Test
    void reversesSubsegment() {
        var move = new InversionMove();
        var input = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
        for (int seed = 0; seed < 100; seed++) {
            var result = move.apply(input, new Random(seed));

            var low = -1;
            var high = -1;
            for (int i = 0; i < input.length; i++) {
                if (input[i] != result[i]) {
                    if (low == -1) {
                        low = i;
                    }
                    high = i;
                }
            }

            if (low == -1) {
                continue;
            }

            assertTrue(low < high, "Seed " + seed + ": low must be less than high");
            for (int i = low; i <= high; i++) {
                assertEquals(
                        input[high - (i - low)],
                        result[i],
                        "Seed " + seed + ": reversed segment mismatch at index " + i);
            }
        }
    }
}
