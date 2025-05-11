package randomProblemGenerator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

public class DistributionsJsonLoader {
    private final JsonObject jsonObject;
    private final Random random;
    private final int seed;
    private Map<Integer, Map<Integer, RandomDrawDistribution<Double>>> ratingDistribution;
    private RandomDrawDistribution<Double> inventoryDurationDistribution;
    private RandomDrawDistribution<Double> commercialDurationDistribution;
    private Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>> pricingDistribution;
    private RandomDrawDistribution<PRICING_TYPE> pricingTypeDistribution;
    private RandomDrawDistribution<ATTENTION> attentionDistribution;
    private RandomDrawDistribution<Integer> audienceTypeDistribution;
    private RandomDrawDistribution<Integer> groupDistribution;

    private List<Integer> audienceTypes;

    public DistributionsJsonLoader(Path filePath, int seed) throws FileNotFoundException {
        var reader = new FileReader(filePath.toFile());
        jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        this.seed = seed;
        this.random = new Random(seed);
    }

    public RandomGeneratorConfig load(
            int nCommercial, int nInventory, int nHours, double suitableInvProbability) {
        loadAudienceTypes();
        loadRatingDistribution();
        loadInventoryDurationDistribution();
        loadCommercialDurationDistribution();
        loadPricingDistribution();
        loadPricingTypeDistribution();
        loadAttentionDistribution();
        loadAudienceTypeDistribution();
        loadGroupDistribution();

        return new RandomGeneratorConfig(
                random,
                seed,
                nCommercial,
                nInventory,
                nHours,
                suitableInvProbability,
                audienceTypes,
                ratingDistribution,
                inventoryDurationDistribution,
                commercialDurationDistribution,
                pricingDistribution,
                pricingTypeDistribution,
                attentionDistribution,
                audienceTypeDistribution,
                groupDistribution);
    }

    private void loadAudienceTypes() {
        audienceTypes = new ArrayList<>();
        var values = jsonObject.getAsJsonArray("audience_type_list");
        for (var value : values) {
            audienceTypes.add(value.getAsInt());
        }

        Collections.sort(audienceTypes);
    }

    private void loadRatingDistribution() {
        ratingDistribution = new HashMap<>();

        var ratingsJObject = jsonObject.getAsJsonObject("ratings");
        for (var entry : ratingsJObject.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var minuteData = entry.getValue().getAsJsonObject();
            for (var minuteEntry : minuteData.entrySet()) {
                var minute = Integer.parseInt(minuteEntry.getKey());
                var distributionObject = minuteEntry.getValue().getAsJsonObject();
                var distribution =
                        new RandomDrawDistributionBuilder<Double>()
                                .setValues(distributionObject, Double::parseDouble)
                                .setRandom(random)
                                .build();

                ratingDistribution
                        .computeIfAbsent(audienceType, t -> new HashMap<>())
                        .put(minute, distribution);
            }
        }
    }

    private void loadInventoryDurationDistribution() {
        var invDurationObj = jsonObject.getAsJsonObject("inventory_duration");
        inventoryDurationDistribution =
                new RandomDrawDistributionBuilder<Double>()
                        .setValues(invDurationObj, Double::parseDouble)
                        .setRandom(random)
                        .build();
    }

    private void loadCommercialDurationDistribution() {
        var commDurationObj = jsonObject.getAsJsonObject("commercial_duration");
        commercialDurationDistribution =
                new RandomDrawDistributionBuilder<Double>()
                        .setValues(commDurationObj, Double::parseDouble)
                        .setRandom(random)
                        .build();
    }

    private void loadPricingDistribution() {
        pricingDistribution = new HashMap<>();

        var pricingJObject = jsonObject.getAsJsonObject("commercial_price");
        for (var entry : pricingJObject.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var pricingTypeData = entry.getValue().getAsJsonObject();

            Map<PRICING_TYPE, RandomDrawDistribution<Double>> audienceTypePricing = new HashMap<>();
            for (var ptEntry : pricingTypeData.entrySet()) {
                PRICING_TYPE pricingType = PRICING_TYPE.fromString(ptEntry.getKey());
                var distributionObject = ptEntry.getValue().getAsJsonObject();

                var distribution =
                        new RandomDrawDistributionBuilder<Double>()
                                .setValues(distributionObject, Double::parseDouble)
                                .setRandom(random)
                                .build();

                audienceTypePricing.put(pricingType, distribution);
            }

            pricingDistribution.put(audienceType, audienceTypePricing);
        }
    }

    private void loadPricingTypeDistribution() {
        var pricingTypeObj = jsonObject.getAsJsonObject("pricing_type");
        pricingTypeDistribution =
                new RandomDrawDistributionBuilder<PRICING_TYPE>()
                        .setValues(pricingTypeObj, PRICING_TYPE::fromString)
                        .setRandom(random)
                        .build();
    }

    private void loadAttentionDistribution() {
        var attentionObj = jsonObject.getAsJsonObject("commercial_flags");
        attentionDistribution =
                new RandomDrawDistributionBuilder<ATTENTION>()
                        .setValues(attentionObj, ATTENTION::fromString)
                        .setRandom(random)
                        .build();
    }

    private void loadAudienceTypeDistribution() {
        var audienceTypeObj = jsonObject.getAsJsonObject("audience_type");
        audienceTypeDistribution =
                new RandomDrawDistributionBuilder<Integer>()
                        .setValues(audienceTypeObj, Integer::parseInt)
                        .setRandom(random)
                        .build();
    }

    private void loadGroupDistribution() {
        var groupObj = jsonObject.getAsJsonObject("commercial_groups");
        groupDistribution =
                new RandomDrawDistributionBuilder<Integer>()
                        .setValues(groupObj, Integer::parseInt)
                        .setRandom(random)
                        .build();
    }
}
