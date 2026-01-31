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
    private RandomDrawDistribution<InventorySample> inventoryDistribution;
    private RandomDrawDistribution<Double> commercialDurationDistribution;
    private Map<Integer, Map<PRICING_TYPE, RandomDrawDistribution<Double>>> pricingDistribution;
    private Map<Integer, RandomDrawDistribution<PRICING_TYPE>> pricingTypeDistribution;
    private Map<Integer, RandomDrawDistribution<ATTENTION>> attentionDistribution;
    private RandomDrawDistribution<Integer> audienceTypeDistribution;
    private RandomDrawDistribution<Integer> groupDistribution;
    private Map<Integer, BinomialDistribution> suitableInvDistribution;

    private List<Integer> audienceTypes;

    public DistributionsJsonLoader(Path filePath, int seed) throws FileNotFoundException {
        var reader = new FileReader(filePath.toFile());
        jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
        this.seed = seed;
        this.random = new Random(seed);
    }

    public RandomGeneratorConfig load(int nInventory, int nHours, double density) {
        loadAudienceTypes();
        loadInventoryDistribution();
        loadCommercialDurationDistribution();
        loadPricingDistribution();
        loadPricingTypeDistribution();
        loadAttentionDistribution();
        loadAudienceTypeDistribution();
        loadGroupDistribution();
        loadSuitableInvDistribution();

        return new RandomGeneratorConfig(
                seed,
                nInventory,
                nHours,
                density,
                audienceTypes,
                inventoryDistribution,
                commercialDurationDistribution,
                pricingDistribution,
                pricingTypeDistribution,
                attentionDistribution,
                audienceTypeDistribution,
                groupDistribution,
                suitableInvDistribution,
                random);
    }

    private void loadAudienceTypes() {
        audienceTypes = new ArrayList<>();
        var values = jsonObject.getAsJsonArray("audience_types");
        for (var value : values) {
            audienceTypes.add(value.getAsInt());
        }

        Collections.sort(audienceTypes);
    }

    private void loadInventoryDistribution() {
        var invDistObj = jsonObject.getAsJsonObject("inventory_dist");
        var params = invDistObj.getAsJsonArray("parameters");
        var samples = new ArrayList<InventorySample>();
        for (var param : params) {
            var obj = param.getAsJsonObject();
            int duration = obj.get("duration").getAsInt();
            var ratingsArray = obj.getAsJsonArray("ratings");
            Map<Integer, Map<Integer, Double>> ratings = new HashMap<>();
            for (var r : ratingsArray) {
                var rObj = r.getAsJsonObject();
                int minute = rObj.get("minute").getAsInt();
                int audienceType = rObj.get("audience_type").getAsInt();
                double rating = rObj.get("rating").getAsDouble();
                ratings.computeIfAbsent(minute, k -> new HashMap<>()).put(audienceType, rating);
            }
            samples.add(new InventorySample(duration, ratings));
        }
        inventoryDistribution = new RandomDrawDistribution<>(samples, random);
    }

    private void loadCommercialDurationDistribution() {
        var commDurationObj = jsonObject.getAsJsonObject("commercial_duration_dist");
        commercialDurationDistribution =
                new RandomDrawDistributionBuilder<Double>()
                        .setValues(commDurationObj, Double::parseDouble)
                        .setRandom(random)
                        .build();
    }

    private void loadPricingDistribution() {
        pricingDistribution = new HashMap<>();

        var pricingJObject = jsonObject.getAsJsonObject("commercial_price_dist");
        for (var entry : pricingJObject.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var pricingTypeData = entry.getValue().getAsJsonObject();

            Map<PRICING_TYPE, RandomDrawDistribution<Double>> audienceTypePricing = new HashMap<>();
            for (var ptEntry : pricingTypeData.entrySet()) {
                PRICING_TYPE pricingType = PRICING_TYPE.valueOf(ptEntry.getKey());
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

        var pricingTypeObj = jsonObject.getAsJsonObject("commercial_pricing_type_dist");
        for (var entry : pricingTypeObj.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var distributionObject = entry.getValue().getAsJsonObject();

            var distribution =
                    new RandomDrawDistributionBuilder<PRICING_TYPE>()
                            .setValues(distributionObject, PRICING_TYPE::valueOf)
                            .setRandom(random)
                            .build();

            pricingTypeDistribution.put(audienceType, distribution);
        }
    }

    private void loadAttentionDistribution() {
        attentionDistribution = new HashMap<>();

        var attentionObj = jsonObject.getAsJsonObject("commercial_flag_dist");
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
        var audienceTypeObj = jsonObject.getAsJsonObject("commercial_audience_type_dist");
        audienceTypeDistribution =
                new RandomDrawDistributionBuilder<Integer>()
                        .setValues(audienceTypeObj, Integer::parseInt)
                        .setRandom(random)
                        .build();
    }

    private void loadGroupDistribution() {
        var groupObj = jsonObject.getAsJsonObject("commercial_group_dist");
        groupDistribution =
                new RandomDrawDistributionBuilder<Integer>()
                        .setValues(groupObj, Integer::parseInt)
                        .setRandom(random)
                        .build();
    }

    private void loadSuitableInvDistribution() {
        suitableInvDistribution = new HashMap<>();
        var suitabilityObj = jsonObject.getAsJsonObject("commercial_inv_suitability_dist");
        for (var entry : suitabilityObj.entrySet()) {
            var audienceType = Integer.parseInt(entry.getKey());
            var distributionObj = entry.getValue().getAsJsonObject();
            var p = distributionObj.get("p").getAsDouble();
            var rng = new JDKRandomGenerator(seed + audienceType);
            suitableInvDistribution.put(audienceType, new BinomialDistribution(rng, 1, p));
        }
    }
}
