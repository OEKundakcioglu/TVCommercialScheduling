package scheduling.solver.heuristic.beecolony.vrp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VrpConverter {

    public static VrpProblem convert(Problem problem) {
        var customers = buildCustomers(problem);
        var vehicles = buildVehicles(problem);
        var depot = new Depot(customers.length);
        var distance = buildDistanceMatrix(problem, customers, vehicles, depot);
        return new VrpProblem(customers, vehicles, depot, distance, problem);
    }

    private static Customer[] buildCustomers(Problem problem) {
        var commercials = problem.getCommercials();
        var customers = new Customer[commercials.length];
        for (int i = 0; i < commercials.length; i++) {
            var comm = commercials[i];
            customers[i] = new Customer(comm.getId(), comm.getDuration(), comm.getGroup());
        }
        return customers;
    }

    private static Vehicle[] buildVehicles(Problem problem) {
        var inventories = problem.getInventories();
        var vehicles = new Vehicle[inventories.length];
        for (int i = 0; i < inventories.length; i++) {
            var inv = inventories[i];
            vehicles[i] = new Vehicle(inv.getId(), inv.getDuration(), inv.getHour());
        }
        return vehicles;
    }

    private static double[][][] buildDistanceMatrix(
            Problem problem, Customer[] customers, Vehicle[] vehicles, Depot depot) {
        int numNodes = customers.length + 1;
        var distance = new double[vehicles.length][numNodes][numNodes];
        for (int v = 0; v < vehicles.length; v++) {
            for (int from = 0; from < numNodes; from++) {
                for (int to = 0; to < numNodes; to++) {
                    distance[v][from][to] = computeDistance(problem, from, to, v, customers, depot);
                }
            }
        }
        return distance;
    }

    private static double computeDistance(
            Problem problem,
            int fromId,
            int toId,
            int vehicleId,
            Customer[] customers,
            Depot depot) {
        var isFromDepot = fromId == depot.id();
        var isToDepot = toId == depot.id();

        if (isFromDepot && isToDepot) {
            return 0.0;
        }
        if (isToDepot) {
            return 0.0;
        }
        if (isFromDepot) {
            if (!problem.isSuitable(toId, vehicleId)) {
                return Double.POSITIVE_INFINITY;
            }
            return 0.0;
        }

        if (fromId == toId) {
            return Double.POSITIVE_INFINITY;
        }
        if (!problem.isSuitable(fromId, vehicleId)) {
            return Double.POSITIVE_INFINITY;
        }
        if (!problem.isSuitable(toId, vehicleId)) {
            return Double.POSITIVE_INFINITY;
        }
        if (customers[fromId].group() == customers[toId].group()) {
            return Double.POSITIVE_INFINITY;
        }
        if (hasSolelyL1Attention(problem, fromId, vehicleId)) {
            return Double.POSITIVE_INFINITY;
        }
        if (hasSolelyF1Attention(problem, toId, vehicleId)) {
            return Double.POSITIVE_INFINITY;
        }
        return 0.0;
    }

    private static boolean hasSolelyF1Attention(Problem problem, int commId, int invId) {
        var types = problem.getAttentionTypes(commId, invId);
        return types.length == 1 && types[0] == AttentionType.F1;
    }

    private static boolean hasSolelyL1Attention(Problem problem, int commId, int invId) {
        var types = problem.getAttentionTypes(commId, invId);
        return types.length == 1 && types[0] == AttentionType.L1;
    }
}
