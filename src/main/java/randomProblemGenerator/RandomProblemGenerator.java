package randomProblemGenerator;

import data.Commercial;
import data.Inventory;
import data.ProblemParameters;
import data.enums.ATTENTION;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    private final Set<Integer> targetAudiences;

    public RandomProblemGenerator(RandomGeneratorConfig config) {
        this.config = config;

        this.setOfCommercials = new ArrayList<>();
        this.setOfInventories = new ArrayList<>();
        this.setOfHours = IntStream.range(0, config.getnHours()).boxed().toList();
        this.setOfFirstCommercials = new ArrayList<>();
        this.setOfLastCommercials = new ArrayList<>();
        this.setOfF30Commercials = new ArrayList<>();
        this.setOfF60Commercials = new ArrayList<>();
        this.targetAudiences = new HashSet<>();
        this.ratings = new HashMap<>();
    }

    public ProblemParameters generate() {
        generateInventories();
        generateCommercials();

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
            var sample = config.sampleInventory();
            int duration = sample.duration();
            var hour = config.assignHourForInventory(duration);
            var inventory = new Inventory(i, duration, hour, 100);
            var invRatings = sample.ratings();

            for (var minuteEntry : sample.ratings().entrySet()) {
                int minute = minuteEntry.getKey();
                for (var audEntry : minuteEntry.getValue().entrySet()) {
                    int audienceType = audEntry.getKey();
                    targetAudiences.add(audienceType);

                    double rating = audEntry.getValue();
                    inventory.ratings.computeIfAbsent(minute, k -> new HashMap<>()).put(audienceType, rating);
                }
            }

            setOfInventories.add(inventory);
            ratings.put(inventory, invRatings);
        }

        var maxTargetAudience = Collections.max(targetAudiences);
        for (var inventory : setOfInventories) {
            inventory.createArrayRatings(maxTargetAudience);
        }
    }

    private void generateCommercials() {
        double totalInvDuration =
                setOfInventories.stream().mapToDouble(Inventory::getDuration).sum();
        double totalCommercialDuration = 0;

        var id = new AtomicInteger(0);
        while (totalCommercialDuration < totalInvDuration * config.getDensity()) {
            int duration = (int) config.sampleCommercialDuration();
            var audienceType = config.sampleAudienceType(targetAudiences);
            var pricingType = config.samplePricingType(audienceType);
            double price = config.samplePrice(audienceType, pricingType);

            var group = config.sampleGroup();
            var commercial =
                    new Commercial(
                            id.getAndIncrement(),
                            duration,
                            price,
                            pricingType,
                            audienceType,
                            group);
            commercial.setAttentionMapArray(new ATTENTION[setOfInventories.size()]);

            setOfCommercials.add(commercial);

            var suitableInventories = getSuitableInventories(audienceType);
            for (var inventory : suitableInventories) {
                var attention = config.sampleAttention(audienceType);
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

            totalCommercialDuration += duration;
        }
    }

    private List<Inventory> getSuitableInventories(int audienceType) {
        var suitableInventories = new ArrayList<Inventory>();

        for (var inv : setOfInventories) {
            boolean isSuitable = config.sampleSuitableInv(audienceType);
            if (isSuitable) {
                suitableInventories.add(inv);
            }
        }

        if (suitableInventories.isEmpty()) {
            int randomIndex = config.getRandomInt(setOfInventories.size());
            suitableInventories.add(setOfInventories.get(randomIndex));
        }

        return suitableInventories;
    }
}
