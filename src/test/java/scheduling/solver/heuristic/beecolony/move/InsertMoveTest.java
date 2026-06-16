package scheduling.solver.heuristic.beecolony.move;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class InsertMoveTest {

    @Test
    void preservesAllElements() {
        var move = new InsertMove();
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
        var move = new InsertMove();
        var input = new int[] {5, 3, 1, 4, 2};
        var original = input.clone();
        move.apply(input, new Random(42));
        assertArrayEquals(original, input);
    }

    @Test
    void resultLengthMatchesInput() {
        var move = new InsertMove();
        var input = new int[] {10, 20, 30, 40, 50};
        for (int seed = 0; seed < 50; seed++) {
            var result = move.apply(input, new Random(seed));
            assertEquals(input.length, result.length, "Seed " + seed);
        }
    }
}
