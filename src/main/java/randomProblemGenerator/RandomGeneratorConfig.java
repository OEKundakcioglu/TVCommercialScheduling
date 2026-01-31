package randomProblemGenerator;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;
import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class RandomGeneratorConfig {
    private final int seed;
    private final int nInventory;
    private final int nHours;

    private final double density;

    private final RandomDrawDistribution<InventorySample> inventoryDistribution;
    private final RandomDrawDistribution<Double> commercialDurationDistribution;
    private final Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>>
            pricingDistribution;
    private final Map<Integer, RandomDrawDistribution<PRICING_TYPE>> pricingTypeDistribution;
    private final Map<Integer, RandomDrawDistribution<ATTENTION>> attentionDistribution;
    private final RandomDrawDistribution<Integer> audienceTypeDistribution;
    private final RandomDrawDistribution<Integer> groupDistribution;
    private final Map<Integer, BinomialDistribution> suitableInvDistribution;
    private final List<Integer> audienceTypes;
    private final int[] hourOccupancy;
    private final Random random;

    public RandomGeneratorConfig(
            int seed,
            int nInventory,
            int nHours,
            double density,
            List<Integer> audienceTypes,
            RandomDrawDistribution<InventorySample> inventoryDistribution,
            RandomDrawDistribution<Double> commercialDurationDistribution,
            Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>> pricingDistribution,
            Map<Integer, RandomDrawDistribution<PRICING_TYPE>> pricingTypeDistribution,
            Map<Integer, RandomDrawDistribution<ATTENTION>> attentionDistribution,
            RandomDrawDistribution<Integer> audienceTypeDistribution,
            RandomDrawDistribution<Integer> groupDistribution,
            Map<Integer, BinomialDistribution> suitableInvDistribution,
            Random random) {
        this.seed = seed;
        this.nInventory = nInventory;
        this.nHours = nHours;
        this.density = density;

        this.audienceTypes = audienceTypes;
        this.inventoryDistribution = inventoryDistribution;
        this.commercialDurationDistribution = commercialDurationDistribution;
        this.pricingDistribution = pricingDistribution;
        this.pricingTypeDistribution = pricingTypeDistribution;
        this.attentionDistribution = attentionDistribution;
        this.audienceTypeDistribution = audienceTypeDistribution;
        this.groupDistribution = groupDistribution;
        this.suitableInvDistribution = suitableInvDistribution;
        this.hourOccupancy = new int[nHours];
        this.random = random;
    }

    public int getSeed() {
        return seed;
    }

    public int getnInventory() {
        return nInventory;
    }

    public int getnHours() {
        return nHours;
    }

    public double sampleCommercialDuration() {
        return commercialDurationDistribution.sample();
    }

    public InventorySample sampleInventory() {
        return inventoryDistribution.sample();
    }

    public int assignHourForInventory(int inventoryDuration) {
        int minOccupancyHour = 0;
        int minOccupancy = hourOccupancy[0];
        for (int h = 1; h < nHours; h++) {
            if (hourOccupancy[h] < minOccupancy) {
                minOccupancy = hourOccupancy[h];
                minOccupancyHour = h;
            }
        }
        hourOccupancy[minOccupancyHour] += inventoryDuration;
        return minOccupancyHour;
    }

    public double samplePrice(int audienceType, PRICING_TYPE pricingType) {
        return pricingDistribution.get(audienceType).get(pricingType).sample();
    }

    public PRICING_TYPE samplePricingType(int audienceType) {
        return pricingTypeDistribution.get(audienceType).sample();
    }

    public ATTENTION sampleAttention(int audienceType) {
        return attentionDistribution.get(audienceType).sample();
    }

    public int sampleAudienceType(Set<Integer> subset) {
        return audienceTypeDistribution.sample(subset);
    }

    public int sampleGroup() {
        return groupDistribution.sample();
    }

    public boolean sampleSuitableInv(int audienceType) {
        return suitableInvDistribution.get(audienceType).sample() == 1;
    }

    public int getRandomInt(int bound) {
        return random.nextInt(bound);
    }

    public List<Integer> getAudienceTypes() {
        return audienceTypes;
    }

    public double getDensity() {
        return density;
    }
}
