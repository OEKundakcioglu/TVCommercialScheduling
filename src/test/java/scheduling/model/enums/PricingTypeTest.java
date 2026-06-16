package scheduling.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PricingTypeTest {

    @Test
    void valueOfPpr() {
        assertEquals(PricingType.PPR, PricingType.valueOf("PPR"));
    }

    @Test
    void valueOfFixed() {
        assertEquals(PricingType.FIXED, PricingType.valueOf("FIXED"));
    }
}
