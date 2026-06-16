package scheduling.solver.heuristic.beecolony.vrp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VehicleTest {
    @Test
    void storesAllFields() {
        var vehicle = new Vehicle(3, 120, 2);
        assertEquals(3, vehicle.id());
        assertEquals(120, vehicle.timeLimit());
        assertEquals(2, vehicle.hour());
    }
}
