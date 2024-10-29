package solvers.heuristicSolvers.beeColonyYu.data;

public class Depot implements Node{
    private final int id;

    public Depot(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public double distanceTo(Node node, Vehicle vehicle) {
        if (node instanceof Depot){
            return 0;
        }

        Customer customer = (Customer) node;

        if (!customer.isVehicleFeasible(vehicle)) return Double.POSITIVE_INFINITY;

        return 0;
    }
}
