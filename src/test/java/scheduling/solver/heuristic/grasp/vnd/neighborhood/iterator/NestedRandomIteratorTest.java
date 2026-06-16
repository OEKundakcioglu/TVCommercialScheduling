package scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NestedRandomIteratorTest {

    @Test
    void singleLevelIteratesAllElements() {
        var iter = new NestedRandomIterator(new int[] {1, 2, 3}, List.of(), new Random(42));

        var results = collect(iter);

        assertEquals(3, results.size());
        var values = new HashSet<Integer>();
        for (var tuple : results) {
            assertEquals(1, tuple.length);
            values.add(tuple[0]);
        }
        assertEquals(new HashSet<>(List.of(1, 2, 3)), values);
    }

    @Test
    void twoLevelsProducesCartesianProduct() {
        var iter =
                new NestedRandomIterator(
                        new int[] {10, 20}, List.of(parent -> new int[] {1, 2}), new Random(42));

        var results = collect(iter);

        assertEquals(4, results.size());
        var pairs = new HashSet<String>();
        for (var tuple : results) {
            assertEquals(2, tuple.length);
            pairs.add(tuple[0] + "," + tuple[1]);
        }
        assertTrue(pairs.contains("10,1"));
        assertTrue(pairs.contains("10,2"));
        assertTrue(pairs.contains("20,1"));
        assertTrue(pairs.contains("20,2"));
    }

    @Test
    void threeLevelsProducesFullProduct() {
        var iter =
                new NestedRandomIterator(
                        new int[] {1, 2},
                        List.of(a -> new int[] {10, 20}, b -> new int[] {100}),
                        new Random(42));

        var results = collect(iter);

        // 2 × 2 × 1 = 4 tuples
        assertEquals(4, results.size());
        for (var tuple : results) {
            assertEquals(3, tuple.length);
            assertEquals(100, tuple[2]);
        }
    }

    @Test
    void skipsEmptyInnerLevel() {
        var iter =
                new NestedRandomIterator(
                        new int[] {1, 2, 3},
                        List.of(parent -> parent == 2 ? new int[0] : new int[] {10}),
                        new Random(42));

        var results = collect(iter);

        // parent=1 → [10], parent=2 → skip, parent=3 → [10]
        assertEquals(2, results.size());
        var parents = new HashSet<Integer>();
        for (var tuple : results) {
            parents.add(tuple[0]);
            assertEquals(10, tuple[1]);
        }
        assertFalse(parents.contains(2));
    }

    @Test
    void emptyRootProducesNoTuples() {
        var iter =
                new NestedRandomIterator(new int[0], List.of(a -> new int[] {1}), new Random(42));

        assertFalse(iter.hasNext());
    }

    @Test
    void returnedArraysAreIndependentCopies() {
        var iter =
                new NestedRandomIterator(
                        new int[] {1, 2}, List.of(a -> new int[] {10}), new Random(42));

        var first = iter.next();
        var second = iter.next();

        first[0] = 999;
        assertTrue(second[0] == 1 || second[0] == 2);
    }

    @Test
    void dependentFactoryUsesParentValue() {
        var iter =
                new NestedRandomIterator(
                        new int[] {3, 5}, List.of(n -> buildRange(n)), new Random(42));

        var results = collect(iter);

        // parent=3 → [0,1,2] (3 tuples), parent=5 → [0,1,2,3,4] (5 tuples)
        assertEquals(8, results.size());
    }

    private int[] buildRange(int n) {
        var arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }
        return arr;
    }

    private List<int[]> collect(NestedRandomIterator iter) {
        var results = new ArrayList<int[]>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }
        return results;
    }
}
