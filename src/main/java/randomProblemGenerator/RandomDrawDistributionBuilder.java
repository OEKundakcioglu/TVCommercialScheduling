package randomProblemGenerator;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class RandomDrawDistributionBuilder<T> {
    private List<T> values;
    private Random random;

    public RandomDrawDistributionBuilder<T> setValues(JsonObject distributionObject, Function<String, T> valueConverter) {
        var jArray = distributionObject.getAsJsonArray("parameters");
        values = new ArrayList<>();

        for (var jValue : jArray) {
            var value = jValue.getAsString();
            values.add(valueConverter.apply(value));
        }

        return this;
    }

    public RandomDrawDistributionBuilder<T> setRandom(Random random) {
        this.random = random;
        return this;
    }

    public RandomDrawDistribution<T> build() {
        if (values == null || random == null) {
            throw new IllegalStateException("Values and random must be set before building.");
        }

        return new RandomDrawDistribution<>(values, random);
    }
}
