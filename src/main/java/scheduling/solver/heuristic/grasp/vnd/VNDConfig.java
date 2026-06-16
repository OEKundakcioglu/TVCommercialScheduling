package scheduling.solver.heuristic.grasp.vnd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.selector.NeighborhoodSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.SearchStrategy;

@Getter
public class VNDConfig {

    @JsonIgnore private final SearchStrategy strategy;
    @JsonIgnore private final List<Neighborhood> neighborhoods;
    @JsonIgnore private final NeighborhoodSelector selector;
    private final double neighborhoodSkipProbability;

    public VNDConfig(
            SearchStrategy strategy,
            List<Neighborhood> neighborhoods,
            NeighborhoodSelector selector,
            double neighborhoodSkipProbability) {
        this.strategy = strategy;
        this.neighborhoods = List.copyOf(neighborhoods);
        this.selector = selector;
        this.neighborhoodSkipProbability = neighborhoodSkipProbability;
    }

    @JsonProperty("strategy")
    public String strategyName() {
        return strategy.stringDesc();
    }

    @JsonProperty("neighborhoods")
    public List<String> neighborhoodNames() {
        return neighborhoods.stream().map(n -> n.type().name()).toList();
    }

    @JsonProperty("selector")
    public String selectorName() {
        return selector.stringDesc();
    }

    public VNDConfig withFreshSelector() {
        return new VNDConfig(
                strategy, neighborhoods, selector.freshInstance(), neighborhoodSkipProbability);
    }

    public String stringDesc() {
        return "VND["
                + strategy.stringDesc()
                + ", "
                + selector.stringDesc()
                + ", skip="
                + neighborhoodSkipProbability
                + "]";
    }
}
