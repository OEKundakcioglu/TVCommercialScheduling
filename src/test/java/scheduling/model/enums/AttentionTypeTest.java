package scheduling.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttentionTypeTest {

    @Test
    void valueOfAllKeys() {
        var expected =
                new String[] {
                    "N", "F1", "F2", "F3", "F12", "F123", "L1", "L2", "L3", "L12", "L123"
                };
        for (var key : expected) {
            assertEquals(key, AttentionType.valueOf(key).name());
        }
    }

    @Test
    void valuesCount() {
        assertEquals(11, AttentionType.values().length);
    }

    @Test
    void isLType_returnsTrueForLTypes() {
        assertTrue(AttentionType.L1.isLType());
        assertTrue(AttentionType.L2.isLType());
        assertTrue(AttentionType.L3.isLType());
        assertTrue(AttentionType.L12.isLType());
        assertTrue(AttentionType.L123.isLType());
    }

    @Test
    void isLType_returnsFalseForNonLTypes() {
        assertFalse(AttentionType.N.isLType());
        assertFalse(AttentionType.F1.isLType());
        assertFalse(AttentionType.F2.isLType());
        assertFalse(AttentionType.F3.isLType());
        assertFalse(AttentionType.F12.isLType());
        assertFalse(AttentionType.F123.isLType());
    }

    @Test
    void isSatisfiedL3() {
        var types = new AttentionType[] {AttentionType.L3};
        // sequenceLength=5: L3 position is 5-3=2
        assertTrue(AttentionType.anySatisfied(types, 2, 5));
        assertFalse(AttentionType.anySatisfied(types, 0, 5));
        assertFalse(AttentionType.anySatisfied(types, 1, 5));
        assertFalse(AttentionType.anySatisfied(types, 3, 5));
        assertFalse(AttentionType.anySatisfied(types, 4, 5));
        // sequenceLength=3: L3 position is 3-3=0
        assertTrue(AttentionType.anySatisfied(types, 0, 3));
        assertFalse(AttentionType.anySatisfied(types, 1, 3));
        assertFalse(AttentionType.anySatisfied(types, 2, 3));
        // sequenceLength=2: L3 position is 2-3=-1, never satisfied
        assertFalse(AttentionType.anySatisfied(types, 0, 2));
        assertFalse(AttentionType.anySatisfied(types, 1, 2));
    }
}
