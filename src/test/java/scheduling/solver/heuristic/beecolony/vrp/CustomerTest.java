package scheduling.solver.heuristic.beecolony.vrp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CustomerTest {
    @Test
    void storesAllFields() {
        var customer = new Customer(5, 30, 2);
        assertEquals(5, customer.id());
        assertEquals(30, customer.serviceTime());
        assertEquals(2, customer.group());
    }
}
