package scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomArrayIteratorTest {

    @Test
    void iteratesAllElements() {
        var array = new int[] {10, 20, 30, 40, 50};
        var iter = new RandomArrayIterator(array, new Random(42));

        var result = new int[5];
        for (int i = 0; i < 5; i++) {
            result[i] = iter.nextInt();
        }

        assertFalse(iter.hasNext());
        Arrays.sort(result);
        assertArrayEquals(new int[] {10, 20, 30, 40, 50}, result);
    }

    @Test
    void emptyArrayHasNoElements() {
        var iter = new RandomArrayIterator(new int[0], new Random(42));

        assertFalse(iter.hasNext());
    }

    @Test
    void doesNotMutateOriginalArray() {
        var array = new int[] {1, 2, 3};
        var copy = array.clone();
        new RandomArrayIterator(array, new Random(42));

        assertArrayEquals(copy, array);
    }

    @Test
    void singleElementIteratesOnce() {
        var iter = new RandomArrayIterator(new int[] {7}, new Random(42));

        assertEquals(7, iter.nextInt());
        assertFalse(iter.hasNext());
    }
}
