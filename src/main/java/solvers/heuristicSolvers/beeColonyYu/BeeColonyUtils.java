package solvers.heuristicSolvers.beeColonyYu;

import data.ProblemParameters;
import data.Solution;
import data.SolutionData;
import solvers.heuristicSolvers.beeColonyYu.data.Customer;
import solvers.heuristicSolvers.beeColonyYu.data.Depot;
import solvers.heuristicSolvers.beeColonyYu.data.Node;
import solvers.heuristicSolvers.beeColonyYu.data.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class BeeColonyUtils {
    private final OrienteeringData orienteeringData;
    private final ProblemParameters parameters;

    public BeeColonyUtils(OrienteeringData orienteeringData, ProblemParameters parameters) {
        this.orienteeringData = orienteeringData;
        this.parameters = parameters;
    }

    public double calculateFitness(int[] solutionString) {
        double fitness = 0;

        int[] hourUtilizations = new int[orienteeringData.getHours().length];

        var vehicle = orienteeringData.getVehicles().getFirst();
        Node node = orienteeringData.getDepot();
        var time = 0;
        for (int j : solutionString) {
            var currentNode = orienteeringData.getNodes().get(j);
            if (currentNode instanceof Depot) {
                time = 0;
                int nextVehicleId = vehicle.id() + 1;
                if (nextVehicleId >= orienteeringData.getVehicles().size()) {
                    throw new IllegalArgumentException("Vehicle limit exceeded");
                }

                vehicle = orienteeringData.getVehicles().get(nextVehicleId);
                node = orienteeringData.getDepot();
                continue;
            }

            Customer customer = (Customer) currentNode;
            if (isExclude(time, node, customer, vehicle, hourUtilizations[vehicle.hour()])) {
                continue;
            }

            fitness += getScore(node, currentNode, time, vehicle);
            time += ((Customer) currentNode).getServiceTime();
            hourUtilizations[vehicle.hour()] += ((Customer) currentNode).getServiceTime();
            node = currentNode;
        }

        return fitness;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isExclude(int time, Node from, Customer customer, Vehicle vehicle, int hourUtilization) {
        if (hourUtilization + customer.getServiceTime() > 720) return true;
        if (Double.isInfinite(from.distanceTo(customer, vehicle))) return true;
        if (time + customer.getServiceTime() > vehicle.timeLimit()) return true;
        if (time > customer.getLatestStartTime(vehicle)) return true;
        if (!customer.isVehicleFeasible(vehicle)) return true;

        return false;
    }

    private double getScore(Node from, Node to, int time, Vehicle vehicle){
        double score = from.distanceTo(to, vehicle);

        if (Double.isInfinite(score)) throw new IllegalArgumentException("Distance is infinite");

        if (to instanceof Customer) {
            score += ((Customer) to).getScore(vehicle, time);
        }

        return score;
    }

    public Solution toSolution(BeeColonySolution beeColonySolution) {
        var solutionDataList = new ArrayList<List<SolutionData>>();
        for (var ignored : orienteeringData.getVehicles()) {
            solutionDataList.add(new ArrayList<>());
        }
        int[] hourUtilizations = new int[orienteeringData.getHours().length];

        var vehicle = orienteeringData.getVehicles().getFirst();
        Node node = orienteeringData.getDepot();
        var time = 0;
        for (var i = 1; i < beeColonySolution.getSolutionString().length-1; i++) {
            var currentNode = orienteeringData.getNodes().get(beeColonySolution.getSolutionString()[i]);
            if (currentNode instanceof Depot) {
                time = 0;
                int nextVehicleId = vehicle.id() + 1;
                if (nextVehicleId >= orienteeringData.getVehicles().size()) {
                    break;
                }

                vehicle = orienteeringData.getVehicles().get(nextVehicleId);
                continue;
            }

            Customer customer = (Customer) currentNode;
            if (isExclude(time, node, customer, vehicle, hourUtilizations[vehicle.hour()])) {
                continue;
            }

            time += ((Customer) currentNode).getServiceTime();
            hourUtilizations[vehicle.hour()] += ((Customer) currentNode).getServiceTime();
            node = currentNode;


            Vehicle finalVehicle = vehicle;
            var data = new SolutionData(
                    parameters.getSetOfCommercials().stream()
                            .filter(commercial -> commercial.getId() == customer.getId()).findFirst().orElseThrow(),
                    parameters.getSetOfInventories().stream()
                            .filter(inventory -> inventory.getId() == finalVehicle.id()).findFirst().orElseThrow()
            );


            solutionDataList.get(vehicle.id()).add(data);
        }

        return new Solution(solutionDataList);
    }
}
