package scheduling.solver.heuristic.grasp.vnd.selector;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;

public class AdaptiveSelector implements NeighborhoodSelector {

    private final double minProbability;
    private final List<Neighborhood> neighborhoods;
    private final double[] probabilities;
    private final double[] gains;
    private final IdentityHashMap<Neighborhood, Integer> indexMap;

    public AdaptiveSelector(double minProbability, List<Neighborhood> neighborhoods) {
        Preconditions.checkArgument(
                minProbability >= 0 && minProbability < 1, "minProbability must be in [0, 1)");
        this.minProbability = minProbability;
        this.neighborhoods = List.copyOf(neighborhoods);
        this.probabilities = new double[neighborhoods.size()];
        Arrays.fill(probabilities, 1.0 / neighborhoods.size());
        this.gains = new double[neighborhoods.size()];
        this.indexMap = new IdentityHashMap<>();
        for (int i = 0; i < neighborhoods.size(); i++) {
            indexMap.put(neighborhoods.get(i), i);
        }
    }

    @Override
    public String stringDesc() {
        return "ADAPTIVE(min=" + minProbability + ")";
    }

    @Override
    public Neighborhood select(List<Neighborhood> neighborhoods, Random random) {
        return neighborhoods.get(selectByProbability(random));
    }

    @Override
    public void reportResult(Neighborhood neighborhood, double revenueGain) {
        var idx = Objects.requireNonNull(indexMap.get(neighborhood));
        gains[idx] += revenueGain;
    }

    @Override
    public void update() {
        recalculateProbabilities();
    }

    @Override
    public NeighborhoodSelector freshInstance() {
        return new AdaptiveSelector(minProbability, neighborhoods);
    }

    private int selectByProbability(Random random) {
        var rand = random.nextDouble();
        var cumulative = 0.0;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return i;
            }
        }
        return probabilities.length - 1;
    }

    private void recalculateProbabilities() {
        var total = Arrays.stream(gains).sum();
        var n = gains.length;
        for (int i = 0; i < n; i++) {
            probabilities[i] = minProbability + (1 - minProbability * n) * gains[i] / total;
        }
    }
}
