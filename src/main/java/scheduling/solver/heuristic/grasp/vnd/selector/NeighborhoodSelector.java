package scheduling.solver.heuristic.grasp.vnd.selector;

import java.util.List;
import java.util.Random;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;

public interface NeighborhoodSelector {

    Neighborhood select(List<Neighborhood> neighborhoods, Random random);

    void reportResult(Neighborhood neighborhood, double revenueGain);

    String stringDesc();

    default void update() {}

    default NeighborhoodSelector freshInstance() {
        return this;
    }
}
