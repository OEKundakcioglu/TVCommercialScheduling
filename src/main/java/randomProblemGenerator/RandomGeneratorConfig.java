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
    private final int nCommercial;
    private final int nInventory;
    private final int nHours;
    private final Map<Integer, Map<Integer, RandomDrawDistribution<Double>>> ratingDistribution;
    private final RandomDrawDistribution<Double> inventoryDurationDistribution;
    private final RandomDrawDistribution<Double> commercialDurationDistribution;
    private final Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>>
            pricingDistribution;
    private final RandomDrawDistribution<PRICING_TYPE> pricingTypeDistribution;
    private final RandomDrawDistribution<ATTENTION> attentionDistribution;
    private final RandomDrawDistribution<Integer> audienceTypeDistribution;
    private final RandomDrawDistribution<Integer> groupDistribution;
    private final BinomialDistribution suitableInvDistribution;
    private final List<Integer> audienceTypes;

    public RandomGeneratorConfig(
            Random rand,
            int seed,
            int nCommercial,
            int nInventory,
            int nHours,
            double suitableInvProbability,
            List<Integer> audienceTypes,
            Map<Integer, Map<Integer, RandomDrawDistribution<Double>>> ratingDistribution,
            RandomDrawDistribution<Double> inventoryDurationDistribution,
            RandomDrawDistribution<Double> commercialDurationDistribution,
            Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>>
                    pricingDistribution,
            RandomDrawDistribution<PRICING_TYPE> pricingTypeDistribution,
            RandomDrawDistribution<ATTENTION> attentionDistribution,
            RandomDrawDistribution<Integer> audienceTypeDistribution,
            RandomDrawDistribution<Integer> groupDistribution) {
        this.seed = seed;
        this.nCommercial = nCommercial;
        this.nInventory = nInventory;
        this.nHours = nHours;
        this.ratingDistribution = ratingDistribution;
        this.audienceTypes = audienceTypes;
        this.inventoryDurationDistribution = inventoryDurationDistribution;
        this.commercialDurationDistribution = commercialDurationDistribution;
        this.pricingDistribution = pricingDistribution;
        this.pricingTypeDistribution = pricingTypeDistribution;
        this.attentionDistribution = attentionDistribution;
        this.audienceTypeDistribution = audienceTypeDistribution;
        this.groupDistribution = groupDistribution;
        this.suitableInvDistribution = new BinomialDistribution(1, suitableInvProbability);
        this.rand = rand;
    }

    public int getSeed() {
        return seed;
    }

    public int getnCommercial() {
        return nCommercial;
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

    public PRICING_TYPE samplePricingType() {
        return pricingTypeDistribution.sample();
    }

    public ATTENTION sampleAttention() {
        return attentionDistribution.sample();
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
}
