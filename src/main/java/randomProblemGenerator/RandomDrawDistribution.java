package randomProblemGenerator;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomDrawDistribution<T> implements IDistribution<T> {
    private final List<T> values;
    private final Random rand;

    public RandomDrawDistribution(List<T> values, Random rand) {
        this.values = values;
        this.rand = rand;
    }

    @Override
    public T sample() {
        int randomIndex = rand.nextInt(values.size());
        return values.get(randomIndex);
    }

    public T sample(Set<T> subset) {
        var subsetValues = values.stream().filter(subset::contains).toList();

        return subsetValues.get(rand.nextInt(subsetValues.size()));
    }
}
