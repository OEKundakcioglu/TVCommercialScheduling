package scheduling.solver.heuristic.grasp.vnd.selector;

import java.util.List;
import java.util.Random;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;

public class SequentialSelector implements NeighborhoodSelector {

    private int index;

    @Override
    public Neighborhood select(List<Neighborhood> neighborhoods, Random random) {
        var selected = neighborhoods.get(index % neighborhoods.size());
        index++;
        return selected;
    }

    @Override
    public String stringDesc() {
        return "SEQ";
    }

    @Override
    public void reportResult(Neighborhood neighborhood, double revenueGain) {
        // no-op for sequential selection
    }
}
