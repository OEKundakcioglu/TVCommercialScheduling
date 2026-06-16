package scheduling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void buildsWithDerivedDurationInMinutes() {
        var inventory = new Inventory(0, 311, 1, 15);

        assertEquals(0, inventory.getId());
        assertEquals(311, inventory.getDuration());
        assertEquals(6, inventory.getDurationInMinutes());
        assertEquals(1, inventory.getHour());
        assertEquals(15, inventory.getMaxCommercialCount());
    }

    @Test
    void durationInMinutesRoundsUp() {
        var inventory = new Inventory(1, 60, 0, 10);
        assertEquals(1, inventory.getDurationInMinutes());

        var inventory2 = new Inventory(2, 61, 0, 10);
        assertEquals(2, inventory2.getDurationInMinutes());
    }
}
