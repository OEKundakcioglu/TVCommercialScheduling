package scheduling.solver.mip.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gurobi.gurobi.GRB;
import org.junit.jupiter.api.Test;

class MipStatusTest {

    @Test
    void mapsOptimalStatus() {
        assertEquals("OPTIMAL", MipStatus.label(GRB.Status.OPTIMAL));
    }

    @Test
    void mapsTimeLimitStatus() {
        assertEquals("TIME_LIMIT", MipStatus.label(GRB.Status.TIME_LIMIT));
    }

    @Test
    void failsOnUnknownStatus() {
        assertThrows(IllegalArgumentException.class, () -> MipStatus.label(-1));
    }
}
