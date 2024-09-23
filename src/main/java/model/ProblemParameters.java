package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import data.Commercial;
import data.Inventory;
import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.util.*;

public class ProblemParameters {
    private final Logger logger = LogManager.getLogger(ProblemParameters.class);
    private final List<Commercial> setOfCommercials;
    private final List<Inventory> setOfInventories;
    private final List<Integer> setOfHours;
    private final List<Commercial> setOfFirstCommercials;
    private final List<Commercial> setOfLastCommercials;
    private final List<Commercial> setOfF30Commercials;
    private final List<Commercial> setOfF60Commercials;
    private final Map<Inventory, Map<Integer, Map<Integer, Double>>> ratings; // inventory -> minute -> audienceType -> rating

    public ProblemParameters() {
        this.setOfCommercials = new ArrayList<>();
        this.setOfInventories = new ArrayList<>();
        this.setOfHours = new ArrayList<>();
        this.ratings = new HashMap<>();
        this.setOfFirstCommercials = new ArrayList<>();
        this.setOfLastCommercials = new ArrayList<>();
        this.setOfF30Commercials = new ArrayList<>();
        this.setOfF60Commercials = new ArrayList<>();
    }

    public void readData(String jsonPath) throws Exception {
        this.logger.info("Reading data from {}...", jsonPath);
        FileReader reader = new FileReader(jsonPath);
        JsonElement scenarioElement = JsonParser.parseReader(reader);
        JsonObject scenarioObject = scenarioElement.getAsJsonObject();

        JsonArray inventoryArray = scenarioObject.getAsJsonArray("inventories");
        JsonArray commercialArray = scenarioObject.getAsJsonArray("commercials");
        JsonArray ratingArray = scenarioObject.getAsJsonArray("ratings");

        populateInventories(inventoryArray);
        populateCommercials(commercialArray);
        populateRatings(ratingArray);

        this.setOfInventories.sort(Comparator.comparing(Inventory::getId));
        this.setOfCommercials.sort(Comparator.comparing(Commercial::getId));

        this.logger.info("Data read successfully.");
    }

    private void populateInventories(JsonArray inventoryArray) {

        for (JsonElement inventoryElement : inventoryArray) {
            JsonObject inventoryObject = inventoryElement.getAsJsonObject();

            int id = inventoryObject.get("id").getAsInt();
            int duration = inventoryObject.get("duration").getAsInt();
            int hour = inventoryObject.get("hour").getAsInt();

            Inventory inventory = new Inventory(id, duration, hour, 20);

            this.setOfInventories.add(inventory);

            if (!this.setOfHours.contains(hour)) this.setOfHours.add(hour);
        }
    }

    private void populateCommercials(JsonArray commercialArray) throws Exception {
        Map<Commercial, Map<ATTENTION, List<Integer>>> temporary = new HashMap<>();

        for (JsonElement commercialElement : commercialArray) {
            JsonObject commercialObject = commercialElement.getAsJsonObject();

            int id = commercialObject.get("id").getAsInt();
            int duration = commercialObject.get("duration").getAsInt();
            double price = commercialObject.get("price").getAsDouble();
            int audienceType = commercialObject.get("audience_type").getAsInt();
            String stringPricingType = commercialObject.get("pricing_type").getAsString();
            PRICING_TYPE pricingType = switch (stringPricingType) {
                case "CPP" -> PRICING_TYPE.PRR;
                case "FixPrice" -> PRICING_TYPE.FIXED;
                default -> throw new Exception("Unrecognized pricing type");
            };
            JsonObject suitableInvMapObject = commercialObject.get("suitable_inventories").getAsJsonObject();

            Map<ATTENTION, List<Integer>> suitableInvMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> pair : suitableInvMapObject.entrySet()) {
                String attentionString = pair.getKey();
                ATTENTION attention = switch (attentionString) {
                    case "NONE" -> ATTENTION.NONE;
                    case "FIRST" -> ATTENTION.FIRST;
                    case "LAST" -> ATTENTION.LAST;
                    case "F30" -> ATTENTION.F30;
                    case "F60" -> ATTENTION.F60;
                    default -> throw new Exception("Unrecognized attention type");
                };
                JsonArray suitableInvArray = pair.getValue().getAsJsonArray();
                List<Integer> suitableInventories = new ArrayList<>();
                for (JsonElement suitableInvElement : suitableInvArray) {
                    suitableInventories.add(suitableInvElement.getAsInt());
                }
                suitableInvMap.put(attention, suitableInventories);
            }

            var group = commercialObject.get("group").getAsInt();

            Commercial commercial = new Commercial(id, duration, price, pricingType, audienceType, group);

            commercial.setAttentionMapArray(new ATTENTION[this.setOfInventories.size()]);

            temporary.put(commercial, suitableInvMap);
            this.setOfCommercials.add(commercial);
        }

        if (temporary.size() != this.setOfCommercials.size()) throw new RuntimeException("Commercial size mismatch");
        populateCommercialSuitableInventories(temporary);

        for (Commercial commercial : this.setOfCommercials) {
            for (ATTENTION attention : commercial.getSetOfSuitableInvByAttention().keySet()) {
                switch (attention) {
                    case FIRST -> this.setOfFirstCommercials.add(commercial);
                    case LAST -> this.setOfLastCommercials.add(commercial);
                    case F30 -> this.setOfF30Commercials.add(commercial);
                    case F60 -> this.setOfF60Commercials.add(commercial);
                }
            }
        }

    }

    private void populateCommercialSuitableInventories(Map<Commercial, Map<ATTENTION, List<Integer>>> temporary) {
        for (Map.Entry<Commercial, Map<ATTENTION, List<Integer>>> pair : temporary.entrySet()) {
            Commercial comm = pair.getKey();
            Map<ATTENTION, List<Integer>> inventoriesByAttention = pair.getValue();
            for (Map.Entry<ATTENTION, List<Integer>> attentionListPair : inventoriesByAttention.entrySet()) {
                ATTENTION attention = attentionListPair.getKey();
                List<Integer> inventoryIds = attentionListPair.getValue();
                for (Integer inventoryId : inventoryIds) {
                    Inventory inventory = this.setOfInventories.stream().filter(inventory1 -> inventory1.getId() == inventoryId).findAny().orElse(null);
                    if (inventory == null) {
                        //this.logger.error(String.format("Suitable inventory with id %d for commercial %d couldn't be found", inventoryId, comm.getId()));
                        throw new RuntimeException(String.format("Suitable inventory with id %d for commercial %d couldn't be found", inventoryId, comm.getId()));
                    }
                    if (attention == ATTENTION.F30 && inventory.getDuration() < 30) continue;
                    if (attention == ATTENTION.F60 && inventory.getDuration() < 60) continue;

                    if (!comm.getSetOfSuitableInvByAttention().containsKey(attention)) {
                        comm.getSetOfSuitableInvByAttention().put(attention, new LinkedList<>());
                    }
                    comm.getSetOfSuitableInvByAttention().get(attention).add(inventory);
                    comm.getAttentionMapArray()[inventory.getId()] = attention;

                    Map<Inventory, ATTENTION> attentionMap = comm.getAttentionMap();
                    if (attentionMap.containsKey(inventory))
                        throw new RuntimeException("Attention map already contains inventory");
                    attentionMap.put(inventory, attention);

                    comm.getSetOfSuitableInv().add(inventory);


                    if (!inventory.getSetOfSuitableCommercials().contains(comm)) {
                        inventory.getSetOfSuitableCommercials().add(comm);
                    }
                }
            }
        }
    }

    private void populateRatings(JsonArray ratingArray) {
        for (JsonElement ratingElement : ratingArray) {
            JsonObject ratingObject = ratingElement.getAsJsonObject();

            int inventoryId = ratingObject.get("inventory_id").getAsInt();
            Inventory inventory = this.setOfInventories.stream().filter(inventory1 -> inventory1.getId() == inventoryId).findAny().orElse(null);
            if (inventory == null) {
                continue;
            }

            int minute = ratingObject.get("minute").getAsInt();
            double rating = ratingObject.get("rating").getAsDouble();

            if (!this.ratings.containsKey(inventory)) {
                this.ratings.put(inventory, new HashMap<>());
            }

            int audienceType = ratingObject.get("audience_type").getAsInt();

            if (!this.ratings.get(inventory).containsKey(minute)) {
                this.ratings.get(inventory).put(minute, new HashMap<>());
                inventory.ratings.put(minute, new HashMap<>());
            }
            this.ratings.get(inventory).get(minute).put(audienceType, rating);
            inventory.ratings.get(minute).put(audienceType, rating);
        }

        for (Inventory inventory : this.setOfInventories) {
            inventory.createArrayRatings();
        }
    }

    public List<Commercial> getSetOfCommercials() {
        return setOfCommercials;
    }

    public List<Inventory> getSetOfInventories() {
        return setOfInventories;
    }

    public List<Integer> getSetOfHours() {
        return setOfHours;
    }

    public List<Commercial> getSetOfFirstCommercials() {
        return setOfFirstCommercials;
    }

    public List<Commercial> getSetOfLastCommercials() {
        return setOfLastCommercials;
    }

    public List<Commercial> getSetOfF30Commercials() {
        return setOfF30Commercials;
    }

    public List<Commercial> getSetOfF60Commercials() {
        return setOfF60Commercials;
    }

    public Map<Inventory, Map<Integer, Map<Integer, Double>>> getRatings() {
        return ratings;
    }
}
