package scheduling.solver.heuristic.beecolony;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.solver.Solution;
import scheduling.solver.heuristic.beecolony.vrp.VrpProblem;
import scheduling.solver.heuristic.grasp.GraspSolution;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SolutionStringConverter {

    public static int[] toSolutionString(GraspSolution graspSolution, VrpProblem vrpProblem) {
        var sequences = graspSolution.getSequences();
        var depotId = vrpProblem.getDepot().id();

        var result = new ArrayList<Integer>();
        for (int invId = 0; invId < sequences.length; invId++) {
            for (int commId : sequences[invId]) {
                result.add(commId);
            }
            if (invId < sequences.length - 1) {
                result.add(depotId);
            }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    public static Solution toSolution(BeeColonySolution beeSolution, VrpProblem vrpProblem) {
        var problem = vrpProblem.getProblem();
        var vehicles = vrpProblem.getVehicles();
        var distance = vrpProblem.getDistance();
        var depotId = vrpProblem.getDepot().id();
        var inventories = problem.getInventories();
        var hours = problem.getHours();

        var perInventory = new ArrayList<List<Commercial>>();
        for (int i = 0; i < inventories.length; i++) {
            perInventory.add(new ArrayList<>());
        }

        var perInventoryNodeIds = new int[inventories.length][];
        for (int i = 0; i < inventories.length; i++) {
            perInventoryNodeIds[i] = new int[0];
        }

        int vehicleIndex = 0;
        int lastNodeId = depotId;
        int time = 0;
        double totalRevenue = 0.0;
        var hourUtilizations = new int[Arrays.stream(hours).max().orElse(0) + 1];
        var commercialCounts = new int[vehicles.length];

        for (int nodeId : beeSolution.getSolutionString()) {
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

            var revenue = problem.getRevenue(nodeId, vehicleId, time);
            totalRevenue += revenue;
            perInventoryNodeIds[vehicleId] = append(perInventoryNodeIds[vehicleId], nodeId);
            perInventory.get(vehicleId).add(problem.getCommercial(nodeId));
            time += customer.serviceTime();
            hourUtilizations[hour] += customer.serviceTime();
            commercialCounts[vehicleId]++;
            lastNodeId = nodeId;
        }

        if (!isAttentionFeasible(problem, perInventoryNodeIds)) {
            throw new IllegalStateException("Bee solution violates attention constraints");
        }

        ImmutableMap.Builder<Inventory, List<Commercial>> builder = ImmutableMap.builder();
        for (int i = 0; i < inventories.length; i++) {
            builder.put(inventories[i], ImmutableList.copyOf(perInventory.get(i)));
        }

        return new Solution(builder.build(), totalRevenue);
    }

    private static boolean isExcluded(
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
        if (time + serviceTime > problem.getInventory(vehicleId).getDuration()) {
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

    private static boolean isAttentionFeasible(Problem problem, int[][] perInventoryNodeIds) {
        for (int invId = 0; invId < perInventoryNodeIds.length; invId++) {
            var sequence = perInventoryNodeIds[invId];
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
