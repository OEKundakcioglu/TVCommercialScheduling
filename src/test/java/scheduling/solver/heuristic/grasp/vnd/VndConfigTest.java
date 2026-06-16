package scheduling.solver.heuristic.grasp.vnd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import scheduling.solver.heuristic.grasp.vnd.selector.AdaptiveSelector;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;

class VndConfigTest {

    @Test
    void stringDescAssemblesComponents() {
        var config =
                new VNDConfig(
                        new FirstImprovingStrategy(), List.of(), new SequentialSelector(), 0.0);
        assertEquals("VND[FIRST, SEQ, skip=0.0]", config.stringDesc());
    }

    @Test
    void stringDescWithAdaptiveSelector() {
        var neighborhoods =
                List.<scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood>of();
        var config =
                new VNDConfig(
                        new FirstImprovingStrategy(),
                        neighborhoods,
                        new AdaptiveSelector(0.05, neighborhoods),
                        0.1);
        assertEquals("VND[FIRST, ADAPTIVE(min=0.05), skip=0.1]", config.stringDesc());
    }

    @Test
    void withFreshSelectorReturnsNewConfigWithFreshSelector() {
        var neighborhoods =
                List.<scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood>of();
        var selector = new AdaptiveSelector(0.05, neighborhoods);
        var config = new VNDConfig(new FirstImprovingStrategy(), neighborhoods, selector, 0.1);

        var copy = config.withFreshSelector();

        assertNotSame(config, copy);
        assertNotSame(config.getSelector(), copy.getSelector());
        assertEquals(
                config.getNeighborhoodSkipProbability(), copy.getNeighborhoodSkipProbability());
        assertEquals(config.neighborhoodNames(), copy.neighborhoodNames());
        assertEquals(config.strategyName(), copy.strategyName());
    }
}
