package scheduling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import scheduling.model.enums.PricingType;

class CommercialTest {

    @Test
    void buildsFromFields() {
        var commercial = new Commercial(0, 861, 0, 10, 110.28, PricingType.PPR);

        assertEquals(0, commercial.getId());
        assertEquals(861, commercial.getGroup());
        assertEquals(0, commercial.getAudienceType());
        assertEquals(10, commercial.getDuration());
        assertEquals(110.28, commercial.getPrice(), 1e-10);
        assertEquals(PricingType.PPR, commercial.getPricingType());
    }

    @Test
    void revenueForPpr() {
        var commercial = new Commercial(0, 861, 0, 10, 110.28, PricingType.PPR);

        assertEquals(110.28 * 10 * 2.5, commercial.getRevenue(2.5), 1e-10);
    }

    @Test
    void revenueForFixed() {
        var commercial = new Commercial(6, 10208, 0, 8, 612.68, PricingType.FIXED);

        assertEquals(612.68 * 8, commercial.getRevenue(999.0), 1e-10);
    }
}
