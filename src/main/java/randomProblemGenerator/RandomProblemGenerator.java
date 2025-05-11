package randomProblemGenerator;

import data.Commercial;
import data.Inventory;
import data.ProblemParameters;
import data.enums.ATTENTION;

import java.util.*;
import java.util.stream.IntStream;

public class RandomProblemGenerator {
    private final RandomGeneratorConfig config;
    private final List<Commercial> setOfCommercials;
    private final List<Inventory> setOfInventories;
    private final List<Integer> setOfHours;
    private final List<Commercial> setOfFirstCommercials;
    private final List<Commercial> setOfLastCommercials;
    private final List<Commercial> setOfF30Commercials;
    private final List<Commercial> setOfF60Commercials;
    private final Map<Inventory, Map<Integer, Map<Integer, Double>>>
            ratings; // inventory -> minute -> audienceType -> rating

    public RandomProblemGenerator(RandomGeneratorConfig config) {
        this.config = config;

        this.setOfCommercials = new ArrayList<>();
        this.setOfInventories = new ArrayList<>();
        this.setOfHours = IntStream.range(0, config.getnHours()).boxed().toList();
        this.setOfFirstCommercials = new ArrayList<>();
        this.setOfLastCommercials = new ArrayList<>();
        this.setOfF30Commercials = new ArrayList<>();
        this.setOfF60Commercials = new ArrayList<>();
        this.ratings = new HashMap<>();
    }

    public ProblemParameters generate() {
        generateInventories();
        generateCommercials();
        generateRatings();

        for (var commercial : setOfCommercials) {
            commercial.setSuitableInventoriesArray(setOfInventories.size());
        }
        return new ProblemParameters(
                setOfCommercials,
                setOfInventories,
                setOfHours,
                setOfFirstCommercials,
                setOfLastCommercials,
                setOfF30Commercials,
                setOfF60Commercials,
                ratings,
                String.valueOf(config.getSeed()));
    }

    private void generateInventories() {
        for (int i = 0; i < config.getnInventory(); i++) {
            int duration = (int) config.sampleInventoryDuration();
            var hour = config.sampleHour();

            var inventory = new Inventory(i, duration, hour, 100);
            setOfInventories.add(inventory);
        }
    }

    private void generateCommercials() {
        for (int i = 0; i < config.getnCommercial(); i++) {
            int duration = (int) config.sampleCommercialDuration();
            var pricingType = config.samplePricingType();
            var audienceType = config.sampleAudienceType();
            double price = config.samplePrice(audienceType, pricingType);

            var group = config.sampleGroup();
            var commercial = new Commercial(i, duration, price, pricingType, audienceType, group);
            commercial.setAttentionMapArray(new ATTENTION[setOfInventories.size()]);

            setOfCommercials.add(commercial);

            var suitableInventories = getSuitableInventories();
            for (var inventory : suitableInventories) {
                var attention = config.sampleAttention();
                if (attention == ATTENTION.F30) {
                    setOfF30Commercials.add(commercial);
                } else if (attention == ATTENTION.F60) {
                    setOfF60Commercials.add(commercial);
                } else if (attention == ATTENTION.FIRST) {
                    setOfFirstCommercials.add(commercial);
                } else if (attention == ATTENTION.LAST) {
                    setOfLastCommercials.add(commercial);
                }

                commercial.getAttentionMap().put(inventory, attention);
                commercial.getAttentionMapArray()[inventory.getId()] = attention;

                inventory.getSetOfSuitableCommercials().add(commercial);
                commercial.getSetOfSuitableInv().add(inventory);
            }
        }
    }

    private void generateRatings() {
        for (var inventory : setOfInventories) {
            for (var t = 0; t <= inventory.getDuration(); t += 60) {
                int minute = t / 60 + 1;
                for (var audienceType : config.getAudienceTypes()) {
                    double rating = config.sampleRating(audienceType, minute);
                    ratings.computeIfAbsent(inventory, k -> new HashMap<>())
                            .computeIfAbsent(minute, k -> new HashMap<>())
                            .put(audienceType, rating);

                    inventory
                            .ratings
                            .computeIfAbsent(minute, k -> new HashMap<>())
                            .put(audienceType, rating);
                }
            }
        }

        for (Inventory inventory : this.setOfInventories) {
            inventory.createArrayRatings();
        }
    }

    private List<Inventory> getSuitableInventories() {
        var suitableInventories = new ArrayList<Inventory>();

        for (var inv : setOfInventories) {
            boolean isSuitable = config.sampleSuitableInv();
            if (isSuitable) {
                suitableInventories.add(inv);
            }
        }

        return suitableInventories;
    }
}
