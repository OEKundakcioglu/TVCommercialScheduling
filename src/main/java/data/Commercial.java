package data;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;

import java.util.*;

public class Commercial {
    private final int id;
    private final int duration;
    private final transient List<Inventory> setOfSuitableInv;
    private final Map<Inventory, ATTENTION> attentionMap;
    private transient ATTENTION[] attentionMapArray;
    private final double price;
    private final PRICING_TYPE pricingType;
    private final int audienceType;
    private final int group;

    private transient double[][] revenueMatrix;

    private transient boolean[] suitableInventoriesArray;

    public Commercial(int id, int duration, double price, PRICING_TYPE pricingType, int audienceType, int group) {
        this.id = id;
        this.duration = duration;
        this.price = price;
        this.pricingType = pricingType;
        this.audienceType = audienceType;
        this.group = group;
        this.attentionMap = new HashMap<>();
        this.setOfSuitableInv = new ArrayList<>();
    }

    public double getRevenue(double rating) {
        if (this.pricingType == PRICING_TYPE.FIXED) {
            return this.price * this.duration;
        } else {
            return rating * this.price * this.duration;
        }
    }

    private double calcRevenue(Inventory inventory, int startTime) {
        int minute = startTime / 60 + 1;

        try {
            double rating = inventory.arrayRatings[minute][this.audienceType];

            return this.getRevenue(rating);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public double getRevenue(Inventory inventory, int startTime) {
        return this.revenueMatrix[inventory.getId()][startTime];
    }

    public int getId() {
        return id;
    }

    public ATTENTION[] getAttentionMapArray() {
        return attentionMapArray;
    }

    public void setAttentionMapArray(ATTENTION[] attentionMapArray) {
        this.attentionMapArray = attentionMapArray;
    }

    public int getDuration() {
        return duration;
    }

    public int getAudienceType() {
        return audienceType;
    }

    public int getGroup() {
        return group;
    }

    public Map<Inventory, ATTENTION> getAttentionMap() {
        return attentionMap;
    }

    public List<Inventory> getSetOfSuitableInv() {
        return setOfSuitableInv;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Commercial that = (Commercial) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public PRICING_TYPE getPricingType() {
        return pricingType;
    }

    public double getPrice() {
        return price;
    }

    public void setSuitableInventoriesArray(int maxInvId) {
        this.suitableInventoriesArray = new boolean[maxInvId + 1];
        for (var inv : this.setOfSuitableInv) {
            this.suitableInventoriesArray[inv.getId()] = true;
        }

        int maxInvDur = this.setOfSuitableInv.stream().mapToInt(Inventory::getDuration).max().orElse(0);
        this.revenueMatrix = new double[maxInvId + 1][maxInvDur + 1];
        for (var inv : this.setOfSuitableInv) {
            for (int i = 0; i <= inv.getDuration(); i++) {
                this.revenueMatrix[inv.getId()][i] = this.calcRevenue(inv, i);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInventorySuitable(Inventory inventory) {
        return this.suitableInventoriesArray[inventory.getId()];
    }
}
