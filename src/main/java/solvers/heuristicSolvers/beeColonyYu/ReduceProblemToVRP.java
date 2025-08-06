package solvers.heuristicSolvers.beeColonyYu;

import data.ProblemParameters;
import solvers.heuristicSolvers.beeColonyYu.data.Customer;
import solvers.heuristicSolvers.beeColonyYu.data.Depot;
import solvers.heuristicSolvers.beeColonyYu.data.Node;
import solvers.heuristicSolvers.beeColonyYu.data.Vehicle;

import java.util.ArrayList;

public class ReduceProblemToVRP {

    public static OrienteeringData reduce(ProblemParameters parameters) {
        var vehicles = new ArrayList<Vehicle>();
        for (var inventory : parameters.getSetOfInventories()) {
            var newVehicle =
                    new Vehicle(
                            inventory.getId(),
                            inventory.getDuration(),
                            inventory.getHour(),
                            inventory.arrayRatings);
            vehicles.add(newVehicle);
        }

        var customers = new ArrayList<Customer>();
        for (var commercial : parameters.getSetOfCommercials()) {
            var newCustomer =
                    new Customer(
                            commercial.getId(),
                            commercial.getDuration(),
                            commercial.getGroup(),
                            commercial.getAudienceType(),
                            commercial.getAttentionMapArray(),
                            commercial.getPricingType(),
                            commercial.getPrice(),
                            vehicles,
                            parameters.getSetOfCommercials().size());

            customers.add(newCustomer);
        }

        var depot = new Depot(customers.size());

        var allNodes = new ArrayList<Node>(customers);
        allNodes.add(depot);

        for (var customer : allNodes) {
            for (var vehicle : vehicles) {
                for (var node : allNodes) {
                    customer.distanceTo(node, vehicle);
                }
            }
        }

        return new OrienteeringData(
                vehicles,
                customers,
                depot,
                parameters.getSetOfHours().stream().mapToInt(i -> i).toArray());
    }
}
