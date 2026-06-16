package scheduling.solver.heuristic.grasp.vnd.neighborhood.iterator;

import java.util.PrimitiveIterator;
import java.util.Random;

public class RandomArrayIterator implements PrimitiveIterator.OfInt {

    private final int[] shuffled;
    private int index;

    public RandomArrayIterator(int[] array, Random random) {
        this.shuffled = array.clone();
        shuffle(random);
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < shuffled.length;
    }

    @Override
    public int nextInt() {
        return shuffled[index++];
    }

    private void shuffle(Random random) {
        for (int i = shuffled.length - 1; i > 0; i--) {
            var j = random.nextInt(i + 1);
            var temp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = temp;
        }
    }
}
