package scheduling.solver.heuristic.beecolony;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.solver.heuristic.beecolony.vrp.VrpProblem;

@RequiredArgsConstructor
public class FitnessEvaluator {

    private static final double INVALID_FITNESS = 1e-9;

    private final VrpProblem vrpProblem;

    public double evaluate(int[] solutionString) {
        var problem = vrpProblem.getProblem();
        var vehicles = vrpProblem.getVehicles();
        var distance = vrpProblem.getDistance();
        var depotId = vrpProblem.getDepot().id();
        var hours = problem.getHours();

        double fitness = 0.0;
        var perInventory = new int[vehicles.length][];
        for (int i = 0; i < vehicles.length; i++) {
            perInventory[i] = new int[0];
        }

        int vehicleIndex = 0;
        int lastNodeId = depotId;
        int time = 0;
        var hourUtilizations = new int[Arrays.stream(hours).max().orElse(0) + 1];
        var commercialCounts = new int[vehicles.length];

        for (int nodeId : solutionString) {
            if (nodeId == depotId) {
                vehicleIndex++;
                lastNodeId = depotId;
                time = 0;
                continue;
            }

            var vehicleId = vehicles[vehicleIndex].id();
            var customer = vrpProblem.getCustomers()[nodeId];
            var hour = vehicles[vehicleIndex].hour();

            if (isExcluded(
                    problem,
                    distance,
                    vehicleId,
                    lastNodeId,
                    nodeId,
                    time,
                    customer.serviceTime(),
                    hourUtilizations[hour],
                    commercialCounts[vehicleId])) {
                continue;
            }

            fitness += problem.getRevenue(nodeId, vehicleId, time);
            perInventory[vehicleId] = append(perInventory[vehicleId], nodeId);
            time += customer.serviceTime();
            hourUtilizations[hour] += customer.serviceTime();
            commercialCounts[vehicleId]++;
            lastNodeId = nodeId;
        }

        if (!isAttentionFeasible(problem, perInventory)) {
            return INVALID_FITNESS;
        }

        return fitness;
    }

    private boolean isExcluded(
            Problem problem,
            double[][][] distance,
            int vehicleId,
            int lastNodeId,
            int nodeId,
            int time,
            int serviceTime,
            int hourUtilization,
            int commercialCount) {
        if (commercialCount >= problem.getInventory(vehicleId).getMaxCommercialCount()) {
            return true;
        }
        if (hourUtilization + serviceTime > Problem.HOURLY_BROADCAST_LIMIT) {
            return true;
        }
        if (Double.isInfinite(distance[vehicleId][lastNodeId][nodeId])) {
            return true;
        }
        if (time + serviceTime > vrpProblem.getVehicles()[vehicleId].timeLimit()) {
            return true;
        }
        if (!problem.isSuitable(nodeId, vehicleId)) {
            return true;
        }
        return false;
    }

    private static int[] append(int[] values, int value) {
        var result = Arrays.copyOf(values, values.length + 1);
        result[values.length] = value;
        return result;
    }

    private boolean isAttentionFeasible(Problem problem, int[][] perInventory) {
        for (int invId = 0; invId < perInventory.length; invId++) {
            var sequence = perInventory[invId];
            var sequenceLength = sequence.length;
            for (int position = 0; position < sequenceLength; position++) {
                if (!AttentionType.anySatisfied(
                        problem.getAttentionTypes(sequence[position], invId),
                        position,
                        sequenceLength)) {
                    return false;
                }
            }
        }
        return true;
    }
}
