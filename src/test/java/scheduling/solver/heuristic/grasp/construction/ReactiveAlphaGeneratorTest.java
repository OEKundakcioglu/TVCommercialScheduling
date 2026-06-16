package scheduling.solver.heuristic.grasp.construction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ReactiveAlphaGeneratorTest {

    @Test
    void generateAlpha_returnsValueFromAlphaSet() {
        var generator = new ReactiveAlphaGenerator(new double[] {0.2, 0.5, 0.8});
        var random = new Random(42);

        for (int i = 0; i < 100; i++) {
            var alpha = generator.generateAlpha(random);
            var isValid =
                    Double.compare(alpha, 0.2) == 0
                            || Double.compare(alpha, 0.5) == 0
                            || Double.compare(alpha, 0.8) == 0;
            assertTrue(isValid, "Alpha " + alpha + " is not in the set");
        }
    }

    @Test
    void generateAlpha_uniformDistributionInitially() {
        var generator = new ReactiveAlphaGenerator(new double[] {0.3, 0.6, 0.9});
        var random = new Random(42);
        var counts = new int[3];

        for (int i = 0; i < 9000; i++) {
            var alpha = generator.generateAlpha(random);
            if (Double.compare(alpha, 0.3) == 0) {
                counts[0]++;
            } else if (Double.compare(alpha, 0.6) == 0) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }

        for (int count : counts) {
            assertTrue(count > 2500, "Expected roughly uniform distribution, got " + count);
            assertTrue(count < 3500, "Expected roughly uniform distribution, got " + count);
        }
    }

    @Test
    void update_biasesProbabilitiesBasedOnFeedback() {
        var generator = new ReactiveAlphaGenerator(new double[] {0.3, 0.6, 0.9});

        for (int i = 0; i < 50; i++) {
            generator.feedback(0.3, 100.0);
        }
        generator.update();

        var random = new Random(42);
        var counts = new int[3];
        for (int i = 0; i < 9000; i++) {
            var alpha = generator.generateAlpha(random);
            if (Double.compare(alpha, 0.3) == 0) {
                counts[0]++;
            } else if (Double.compare(alpha, 0.6) == 0) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }

        assertTrue(counts[0] > counts[1], "Alpha 0.3 should be selected more often");
        assertTrue(counts[0] > counts[2], "Alpha 0.3 should be selected more often");
    }

    @Test
    void feedback_doesNotRecalculateWithoutUpdate() {
        var generator = new ReactiveAlphaGenerator(new double[] {0.3, 0.6, 0.9});

        for (int i = 0; i < 50; i++) {
            generator.feedback(0.3, 100.0);
        }

        var random = new Random(42);
        var counts = new int[3];
        for (int i = 0; i < 9000; i++) {
            var alpha = generator.generateAlpha(random);
            if (Double.compare(alpha, 0.3) == 0) {
                counts[0]++;
            } else if (Double.compare(alpha, 0.6) == 0) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }

        for (int count : counts) {
            assertTrue(count > 2500, "Should still be uniform, got " + count);
            assertTrue(count < 3500, "Should still be uniform, got " + count);
        }
    }

    @Test
    void feedback_throwsForUnknownAlpha() {
        var generator = new ReactiveAlphaGenerator(new double[] {0.3, 0.6});
        assertThrows(IllegalArgumentException.class, () -> generator.feedback(0.99, 50.0));
    }

    @Test
    void defaultConstructor_usesNineAlphaValues() {
        var generator = new ReactiveAlphaGenerator();
        var random = new Random(42);
        var alpha = generator.generateAlpha(random);
        assertTrue(alpha >= 0.1 && alpha <= 0.9);
    }

    @Test
    void generateAlpha_withSingleValue_alwaysReturnsThatValue() {
        var generator = new ReactiveAlphaGenerator(new double[] {0.5});
        var random = new Random(42);

        for (int i = 0; i < 10; i++) {
            assertEquals(0.5, generator.generateAlpha(random));
        }
    }
}
