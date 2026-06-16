package scheduling.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.enums.PricingType;

class SolverSolutionTest {

    @Test
    void storesSolutionCheckPointsAndAdditionalInformation() {
        var inv = new Inventory(0, 120, 1, 5);
        var comm = new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR);
        var solution = new Solution(Map.of(inv, List.of(comm)), 500.0);
        var checkPoints = List.of(new CheckPoint(100.0, 0.5), new CheckPoint(500.0, 1.2));
        var info = "test-info";

        var solverSolution = new SolverSolution<>(solution, checkPoints, info);

        assertSame(solution, solverSolution.getBestSolution());
        assertSame(checkPoints, solverSolution.getCheckPoints());
        assertEquals("test-info", solverSolution.getAdditionalInformation());
    }
}
