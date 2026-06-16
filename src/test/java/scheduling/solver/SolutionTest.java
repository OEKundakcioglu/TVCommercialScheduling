package scheduling.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.enums.PricingType;

class SolutionTest {

    @Test
    void storesAssignmentsAndRevenue() {
        var inv = new Inventory(0, 120, 1, 5);
        var comm = new Commercial(0, 1, 0, 10, 100.0, PricingType.PPR);
        var assignments = Map.of(inv, List.of(comm));

        var solution = new Solution(assignments, 500.0);

        assertSame(assignments, solution.getAssignments());
        assertEquals(500.0, solution.getTotalRevenue(), 1e-10);
    }
}
