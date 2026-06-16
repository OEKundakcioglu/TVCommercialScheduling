package scheduling.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.model.enums.AttentionType;

@RequiredArgsConstructor
public class Problem {

    public static final int HOURLY_BROADCAST_LIMIT = 720;

    @Getter private final Commercial[] commercials;
    @Getter private final Inventory[] inventories;
    @Getter private final int[] hours;
    private final boolean[][] suitability;
    private final AttentionType[][][] attentionTypes;
    private final int[][] suitableInventoriesFor;
    private final int[][] suitableCommercialsFor;
    private final double[][][] ratings;
    private final double[][][] revenueMatrix;

    public Commercial getCommercial(int commId) {
        return commercials[commId];
    }

    public Inventory getInventory(int invId) {
        return inventories[invId];
    }

    public AttentionType[] getAttentionTypes(int commId, int invId) {
        return attentionTypes[commId][invId];
    }

    public double getRevenue(int commId, int invId, int startTime) {
        return revenueMatrix[commId][invId][startTime];
    }

    public boolean isSuitable(int commId, int invId) {
        return suitability[commId][invId];
    }

    public int[] getSuitableInventories(int commId) {
        return suitableInventoriesFor[commId];
    }

    public int[] getSuitableCommercials(int invId) {
        return suitableCommercialsFor[invId];
    }

    public double getRating(int invId, int minute, int audienceType) {
        return ratings[invId][minute][audienceType];
    }
}
