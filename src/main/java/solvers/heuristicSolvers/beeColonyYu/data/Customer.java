package solvers.heuristicSolvers.beeColonyYu.data;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;

import java.util.List;

public class Customer implements Node {
    private final int id;
    private final int serviceTime;
    private final int group;
    private final int audienceType;
    private final PRICING_TYPE pricingType;
    private final double price;
    private final ATTENTION[] attentionMapArray;
    private final boolean[] feasibleVehicles;
    private final double[][] distanceMatrix;
    private final boolean[][] calculatedMatrix;

    public Customer(
            int id,
            int serviceTime,
            int group,
            int audienceType,
            ATTENTION[] attentionMapArray,
            PRICING_TYPE pricingType,
            double price,
            List<Vehicle> vehicles,
            int nCustomer) {
        this.id = id;
        this.serviceTime = serviceTime;
        this.group = group;
        this.attentionMapArray = attentionMapArray;
        this.audienceType = audienceType;
        this.pricingType = pricingType;
        this.price = price;

        this.feasibleVehicles = new boolean[vehicles.size()];
        for (int i = 0; i < vehicles.size(); i++) {
            this.feasibleVehicles[i] = false;
        }

        for (var i = 0; i < attentionMapArray.length; i++) {
            if (attentionMapArray[i] != null) this.feasibleVehicles[i] = true;
        }

        this.distanceMatrix = new double[vehicles.size()][nCustomer + 1];
        this.calculatedMatrix = new boolean[vehicles.size()][nCustomer + 1];
    }

    public double distanceTo(Node node, Vehicle vehicle) {
        var calculated = calculatedMatrix[vehicle.id()][node.getId()];
        if (calculated) return distanceMatrix[vehicle.id()][node.getId()];

        var distance = distanceToCalc(node, vehicle);
        distanceMatrix[vehicle.id()][node.getId()] = distance;
        calculatedMatrix[vehicle.id()][node.getId()] = true;
        return distance;
    }

    private double distanceToCalc(Node node, Vehicle vehicle) {
        if (node instanceof Depot) {
            return 0;
        }
        if (node == this) {
            return Double.POSITIVE_INFINITY;
        }

        Customer customer = (Customer) node;

        if (!this.isVehicleFeasible(vehicle)) return Double.POSITIVE_INFINITY;
        if (!customer.isVehicleFeasible(vehicle)) return Double.POSITIVE_INFINITY;
        if (this.group == customer.group) return Double.POSITIVE_INFINITY;
        if (this.getAttention(vehicle) == ATTENTION.LAST) return Double.POSITIVE_INFINITY;
        if (customer.getAttention(vehicle) == ATTENTION.FIRST) return Double.POSITIVE_INFINITY;

        return 0;
    }

    public int getId() {
        return id;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    public double getScore(Vehicle vehicle, double startTime) {
        int minute = (int) startTime / 60 + 1;

        double rating = vehicle.arrayRatings()[minute][this.audienceType];

        if (this.pricingType == PRICING_TYPE.FIXED) {
            return this.price * this.serviceTime;
        } else {
            return rating * this.price * this.serviceTime;
        }
    }

    public int getLatestStartTime(Vehicle vehicle) {
        if (getAttention(vehicle) == ATTENTION.F30) return 30;
        if (getAttention(vehicle) == ATTENTION.F60) return 60;
        if (getAttention(vehicle) == ATTENTION.FIRST) return 0;

        return Integer.MAX_VALUE;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isVehicleFeasible(Vehicle vehicle) {
        return feasibleVehicles[vehicle.id()];
    }

    public ATTENTION getAttention(Vehicle vehicle) {
        return attentionMapArray[vehicle.id()];
    }
}
