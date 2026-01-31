package data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import data.serializers.InventorySerializer;
import data.serializers.JsonSerializableObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = InventorySerializer.class)
public class Inventory extends JsonSerializableObject {
    final int id;
    final int duration;
    final int durationInMinutes;
    final int maxCommercialCount;
    final int hour;
    final transient List<Commercial> setOfSuitableCommercials;
    public transient double[][] arrayRatings;
    public transient final Map<Integer, Map<Integer, Double>> ratings; // minute -> audience -> rating

    public Inventory(int id, int duration, int hour, int maxCommercialCount) {
        this.id = id;
        this.duration = duration;
        this.durationInMinutes = (int) Math.ceil(duration / 60.0);

        this.hour = hour;
        this.maxCommercialCount = maxCommercialCount;
        this.setOfSuitableCommercials = new LinkedList<>();

        this.ratings = new HashMap<>();
    }

    public void createArrayRatings(int lastTargetAudience) {
        var maxRow = this.ratings.size() + 1;

        this.arrayRatings = new double[maxRow][lastTargetAudience + 1];
        for (var kvp1 : this.ratings.entrySet()) {
            var minute = kvp1.getKey();
            var ratings = kvp1.getValue();
            for (var kvp2 : ratings.entrySet()) {
                var audience = kvp2.getKey();
                var rating = kvp2.getValue();
                this.arrayRatings[minute][audience] = rating;
            }
        }
    }

    public int getId() {
        return id;
    }

    public int getDuration() {
        return duration;
    }

    public int getDurationInMinutes() {
        return durationInMinutes;
    }

    public int getMaxCommercialCount() {
        return maxCommercialCount;
    }

    public int getHour() {
        return hour;
    }

    public List<Commercial> getSetOfSuitableCommercials() {
        return setOfSuitableCommercials;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Inventory inventory = (Inventory) obj;
        return id == inventory.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Inventory_" + id;
    }
}
