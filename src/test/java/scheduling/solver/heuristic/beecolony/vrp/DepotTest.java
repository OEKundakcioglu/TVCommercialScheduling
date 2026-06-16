package scheduling.solver.heuristic.beecolony.vrp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DepotTest {
    @Test
    void storesId() {
        var depot = new Depot(10);
        assertEquals(10, depot.id());
    }
}
