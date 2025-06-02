package randomProblemGenerator;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;

import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RandomGeneratorConfig {
    private final Random rand;

    private final int seed;
    private final int nInventory;
    private final int nHours;
    private final double density;

    private final Map<Integer, Map<Integer, RandomDrawDistribution<Double>>> ratingDistribution;
    private final RandomDrawDistribution<Double> inventoryDurationDistribution;
    private final RandomDrawDistribution<Double> commercialDurationDistribution;
    private final Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>>
            pricingDistribution;
    private final Map<Integer, RandomDrawDistribution<PRICING_TYPE>> pricingTypeDistribution;
    private final Map<Integer, RandomDrawDistribution<ATTENTION>> attentionDistribution;
    private final RandomDrawDistribution<Integer> audienceTypeDistribution;
    private final RandomDrawDistribution<Integer> groupDistribution;
    private final BinomialDistribution suitableInvDistribution;
    private final List<Integer> audienceTypes;

    public RandomGeneratorConfig(
            Random rand,
            int seed,
            int nInventory,
            int nHours,
            double density,
            List<Integer> audienceTypes,
            Map<Integer, Map<Integer, RandomDrawDistribution<Double>>> ratingDistribution,
            RandomDrawDistribution<Double> inventoryDurationDistribution,
            RandomDrawDistribution<Double> commercialDurationDistribution,
            Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>> pricingDistribution,
            Map<Integer, RandomDrawDistribution<PRICING_TYPE>> pricingTypeDistribution,
            Map<Integer, RandomDrawDistribution<ATTENTION>> attentionDistribution,
            RandomDrawDistribution<Integer> audienceTypeDistribution,
            RandomDrawDistribution<Integer> groupDistribution,
            BinomialDistribution suitableInvDistribution) {
        this.seed = seed;
        this.nInventory = nInventory;
        this.nHours = nHours;
        this.density = density;

        this.ratingDistribution = ratingDistribution;
        this.audienceTypes = audienceTypes;
        this.inventoryDurationDistribution = inventoryDurationDistribution;
        this.commercialDurationDistribution = commercialDurationDistribution;
        this.pricingDistribution = pricingDistribution;
        this.pricingTypeDistribution = pricingTypeDistribution;
        this.attentionDistribution = attentionDistribution;
        this.audienceTypeDistribution = audienceTypeDistribution;
        this.groupDistribution = groupDistribution;
        this.suitableInvDistribution = suitableInvDistribution;
        this.rand = rand;
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

    public double sampleInventoryDuration() {
        return inventoryDurationDistribution.sample();
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

    public int sampleAudienceType() {
        return audienceTypeDistribution.sample();
    }

    public int sampleGroup() {
        return groupDistribution.sample();
    }

    public int sampleHour() {
        return rand.nextInt(0, nHours);
    }

    public double sampleRating(int audienceType, int minute) {
        return ratingDistribution.get(audienceType).get(minute).sample();
    }

    public boolean sampleSuitableInv() {
        return suitableInvDistribution.sample() == 1;
    }

    public List<Integer> getAudienceTypes() {
        return audienceTypes;
    }

    public double getDensity() {
        return density;
    }
}
