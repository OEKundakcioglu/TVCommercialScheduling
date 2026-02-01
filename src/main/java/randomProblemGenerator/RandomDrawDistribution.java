package randomProblemGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomDrawDistribution<T> implements IDistribution<T> {
    private final List<T> values;
    private final Random rand;

    private final List<T> notSampledValued;

    public RandomDrawDistribution(List<T> values, Random rand) {
        this.values = values;
        this.rand = rand;
        this.notSampledValued = new ArrayList<>(values);
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

    public T sampleWithoutReplacement() {
        var sampleIdx = rand.nextInt(notSampledValued.size());
        var sampledValue = notSampledValued.get(sampleIdx);
        notSampledValued.remove(sampleIdx);
        return sampledValue;
    }
}
