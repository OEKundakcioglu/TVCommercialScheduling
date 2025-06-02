package randomProblemGenerator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;

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
    private Map<Integer, RandomDrawDistribution<PRICING_TYPE>> pricingTypeDistribution;
    private Map<Integer, RandomDrawDistribution<ATTENTION>> attentionDistribution;
    private RandomDrawDistribution<Integer> audienceTypeDistribution;
    private RandomDrawDistribution<Integer> groupDistribution;
    private BinomialDistribution suitableInvDistribution;

    private List<Integer> audienceTypes;

    public DistributionsJsonLoader(Path filePath, int seed) throws FileNotFoundException {
        var reader = new FileReader(filePath.toFile());
        jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        this.seed = seed;
        this.random = new Random(seed);
    }

    public RandomGeneratorConfig load(int nInventory, int nHours, double density) {
        loadAudienceTypes();
        loadRatingDistribution();
        loadInventoryDurationDistribution();
        loadCommercialDurationDistribution();
        loadPricingDistribution();
        loadPricingTypeDistribution();
        loadAttentionDistribution();
        loadAudienceTypeDistribution();
        loadGroupDistribution();
        loadSuitableInvDistribution();

        return new RandomGeneratorConfig(
                random,
                seed,
                nInventory,
                nHours,
                density,
                audienceTypes,
                ratingDistribution,
                inventoryDurationDistribution,
                commercialDurationDistribution,
                pricingDistribution,
                pricingTypeDistribution,
                attentionDistribution,
                audienceTypeDistribution,
                groupDistribution,
                suitableInvDistribution);
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
        pricingTypeDistribution = new HashMap<>();

        var pricingTypeObj = jsonObject.getAsJsonObject("pricing_type");
        for (var entry : pricingTypeObj.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var distributionObject = entry.getValue().getAsJsonObject();

            var distribution =
                    new RandomDrawDistributionBuilder<PRICING_TYPE>()
                            .setValues(distributionObject, PRICING_TYPE::fromString)
                            .setRandom(random)
                            .build();

            pricingTypeDistribution.put(audienceType, distribution);
        }
    }

    private void loadAttentionDistribution() {
        attentionDistribution = new HashMap<>();

        var attentionObj = jsonObject.getAsJsonObject("commercial_flags");
        for (var entry : attentionObj.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var distributionObject = entry.getValue().getAsJsonObject();

            var distribution =
                    new RandomDrawDistributionBuilder<ATTENTION>()
                            .setValues(distributionObject, ATTENTION::fromString)
                            .setRandom(random)
                            .build();

            attentionDistribution.put(audienceType, distribution);
        }
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

    private void loadSuitableInvDistribution() {
        var prob = jsonObject.get("suitability_probability").getAsDouble();
        var rng = new JDKRandomGenerator(seed);
        suitableInvDistribution = new BinomialDistribution(rng, 1, prob);
    }
}
