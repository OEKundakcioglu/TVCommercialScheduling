package solvers.heuristicSolvers.beeColonyYu;

import solvers.heuristicSolvers.beeColonyYu.data.Customer;
import solvers.heuristicSolvers.beeColonyYu.data.Depot;
import solvers.heuristicSolvers.beeColonyYu.data.Node;
import solvers.heuristicSolvers.beeColonyYu.data.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class OrienteeringData {
    private final List<Vehicle> vehicles;
    private final List<Customer> customers;
    private final Depot depot;
    private final List<Node> nodes;
    private final int[] hours;

    public OrienteeringData(List<Vehicle> vehicles, List<Customer> customers, Depot depot, int[] hours) {
        this.vehicles = vehicles;
        this.customers = customers;
        this.depot = depot;

        this.nodes = new ArrayList<>(customers);
        this.nodes.add(depot);
        this.hours = hours;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public Depot getDepot() {
        return depot;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public int[] getHours() {
        return hours;
    }
}
