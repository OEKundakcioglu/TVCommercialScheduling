package data.problemBuilders;

import data.Commercial;
import data.Inventory;
import data.ProblemParameters;
import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;

import org.apache.commons.math3.util.Pair;

import runParameters.RandomGeneratorConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this.setOfHours = new ArrayList<>();
        this.setOfFirstCommercials = new ArrayList<>();
        this.setOfLastCommercials = new ArrayList<>();
        this.setOfF30Commercials = new ArrayList<>();
        this.setOfF60Commercials = new ArrayList<>();
        this.ratings = new HashMap<>();
    }

    public ProblemParameters generate() {
        setOfHours.addAll(config.hourDistribution().getPmf().stream().map(Pair::getKey).toList());
        generateInventories();
        generateRatings();
        generateCommercials();

        return new ProblemParameters(
                setOfCommercials,
                setOfInventories,
                setOfHours,
                setOfFirstCommercials,
                setOfLastCommercials,
                setOfF30Commercials,
                setOfF60Commercials,
                ratings,
                String.valueOf(config.seed()));
    }

    private void generateInventories() {
        for (int i = 0; i < config.nInventory(); i++) {
            int duration = (int) config.commercialDurationDistribution().sample();
            var hour = config.hourDistribution().sample();

            var inventory = new Inventory(i, duration, hour, 100);
            setOfInventories.add(inventory);
        }
    }

    private void generateCommercials() {
        for (int i = 0; i < config.nCommercial(); i++) {
            int duration = (int) config.commercialDurationDistribution().sample();
            double price;
            var pricingType = config.pricingTypeDistribution().sample();
            if (pricingType == PRICING_TYPE.PRR) price = config.pprDistribution().sample();
            else if (pricingType == PRICING_TYPE.FIXED) price = config.fixedDistribution().sample();
            else throw new IllegalArgumentException("Invalid price type: " + pricingType);

            var audienceType = config.audienceTypeDistribution().sample();
            var group = config.groupDistribution().sample();
            var commercial = new Commercial(i, duration, price, pricingType, audienceType, group);
            commercial.setAttentionMapArray(new ATTENTION[setOfInventories.size()]);

            setOfCommercials.add(commercial);

            var suitableInventories = getSuitableInventories();
            for (var inventory : suitableInventories) {
                var attention = config.attentionDistribution().sample();
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

            commercial.setSuitableInventoriesArray(setOfInventories.size());
        }
    }

    private void generateRatings() {
        for (var inventory : setOfInventories) {
            for (var t = 0; t <= inventory.getDuration(); t += 60) {
                int minute = t / 60 + 1;
                for (var audienceType : config.audienceTypes()) {
                    double rating = config.ratingDistribution().sample();
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
            boolean isSuitable = config.suitableInvDistribution().sample() == 1;
            if (isSuitable) {
                suitableInventories.add(inv);
            }
        }

        return suitableInventories;
    }
}
