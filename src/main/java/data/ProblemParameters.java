package data;


import java.util.*;

public class ProblemParameters {
    private final List<Commercial> setOfCommercials;
    private final List<Inventory> setOfInventories;
    private final List<Integer> setOfHours;
    private final List<Commercial> setOfFirstCommercials;
    private final List<Commercial> setOfLastCommercials;
    private final List<Commercial> setOfF30Commercials;
    private final List<Commercial> setOfF60Commercials;
    private final Map<Inventory, Map<Integer, Map<Integer, Double>>> ratings; // inventory -> minute -> audienceType -> rating
    private final String instance;

    public ProblemParameters(
            List<Commercial> setOfCommercials,
            List<Inventory> setOfInventories,
            List<Integer> setOfHours,
            List<Commercial> setOfFirstCommercials,
            List<Commercial> setOfLastCommercials,
            List<Commercial> setOfF30Commercials,
            List<Commercial> setOfF60Commercials,
            Map<Inventory, Map<Integer, Map<Integer, Double>>> ratings,
            String instance) {
        this.setOfCommercials = setOfCommercials;
        this.setOfInventories = setOfInventories;
        this.setOfHours = setOfHours;
        this.setOfFirstCommercials = setOfFirstCommercials;
        this.setOfLastCommercials = setOfLastCommercials;
        this.setOfF30Commercials = setOfF30Commercials;
        this.setOfF60Commercials = setOfF60Commercials;
        this.ratings = ratings;
        this.instance = instance;
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

    public String  getInstance() {
        return instance;
    }
}
