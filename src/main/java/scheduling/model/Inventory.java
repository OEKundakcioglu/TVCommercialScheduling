package scheduling.model;

import lombok.Getter;

@Getter
public class Inventory {

    private final int id;
    private final int duration;
    private final int durationInMinutes;
    private final int hour;
    private final int maxCommercialCount;

    public Inventory(int id, int duration, int hour, int maxCommercialCount) {
        this.id = id;
        this.duration = duration;
        this.durationInMinutes = (int) Math.ceil(duration / 60.0);
        this.hour = hour;
        this.maxCommercialCount = maxCommercialCount;
    }
}
