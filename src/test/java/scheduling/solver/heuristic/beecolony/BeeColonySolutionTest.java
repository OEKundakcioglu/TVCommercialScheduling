package scheduling.solver.heuristic.beecolony;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BeeColonySolutionTest {
    @Test
    void storesSolutionStringAndFitness() {
        var solString = new int[] {0, 3, 1, 2};
        var solution = new BeeColonySolution(solString, 42.5);
        assertArrayEquals(new int[] {0, 3, 1, 2}, solution.getSolutionString());
        assertEquals(42.5, solution.getFitness());
    }
}
