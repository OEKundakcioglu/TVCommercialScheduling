package scheduling.solver.heuristic.grasp.vnd.selector;

import java.util.List;
import java.util.Random;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;

public class ShuffledSelector implements NeighborhoodSelector {

    @Override
    public Neighborhood select(List<Neighborhood> neighborhoods, Random random) {
        return neighborhoods.get(random.nextInt(neighborhoods.size()));
    }

    @Override
    public String stringDesc() {
        return "SHUF";
    }

    @Override
    public void reportResult(Neighborhood neighborhood, double revenueGain) {
        // no-op for shuffled selection
    }
}
