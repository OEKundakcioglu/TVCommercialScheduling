package scheduling.solver.heuristic.beecolony.vrp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import scheduling.model.Problem;

class VrpProblemTest {

    @Test
    void storesAllFields() {
        var customers = new Customer[] {new Customer(0, 10, 0), new Customer(1, 20, 1)};
        var vehicles = new Vehicle[] {new Vehicle(0, 100, 0)};
        var depot = new Depot(2);
        var distance = new double[1][3][3];
        var problem = buildStubProblem();

        var vrp = new VrpProblem(customers, vehicles, depot, distance, problem);

        assertEquals(2, vrp.getCustomers().length);
        assertEquals(1, vrp.getVehicles().length);
        assertEquals(2, vrp.getDepot().id());
        assertEquals(problem, vrp.getProblem());
    }

    private Problem buildStubProblem() {
        return new Problem(
                new scheduling.model.Commercial[0],
                new scheduling.model.Inventory[0],
                new int[0],
                new boolean[0][0],
                new scheduling.model.enums.AttentionType[0][0][],
                new int[0][],
                new int[0][],
                new double[0][0][],
                new double[0][0][]);
    }
}
