package scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

public class NestedRandomIterator implements Iterator<int[]> {

    private final List<IntFunction<int[]>> factories;
    private final Random random;
    private final RandomArrayIterator[] iterators;
    private final int[] current;
    private boolean exhausted;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Factories and Random are intentionally shared, not mutated")
    public NestedRandomIterator(int[] root, List<IntFunction<int[]>> factories, Random random) {
        this.factories = factories;
        this.random = random;
        var depth = factories.size() + 1;
        this.iterators = new RandomArrayIterator[depth];
        this.current = new int[depth];
        iterators[0] = new RandomArrayIterator(root, random);
        this.exhausted = !advanceFrom(0);
    }

    @Override
    public boolean hasNext() {
        return !exhausted;
    }

    @Override
    public int[] next() {
        var result = current.clone();
        if (!advance()) {
            exhausted = true;
        }
        return result;
    }

    private boolean advance() {
        for (int level = iterators.length - 1; level >= 0; level--) {
            if (iterators[level].hasNext()) {
                current[level] = iterators[level].nextInt();
                if (level == iterators.length - 1) {
                    return true;
                }
                if (advanceFrom(level + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean advanceFrom(int level) {
        if (level >= iterators.length) {
            return true;
        }
        if (level == 0) {
            while (iterators[0].hasNext()) {
                current[0] = iterators[0].nextInt();
                if (advanceFrom(1)) {
                    return true;
                }
            }
            return false;
        }
        var parentValue = current[level - 1];
        var childArray = factories.get(level - 1).apply(parentValue);
        iterators[level] = new RandomArrayIterator(childArray, random);
        while (iterators[level].hasNext()) {
            current[level] = iterators[level].nextInt();
            if (advanceFrom(level + 1)) {
                return true;
            }
        }
        return false;
    }
}
