package scheduling.solver.heuristic.beecolony;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.RunInfo;

class BeeColonyAlgorithmTest {

    @Test
    void producesValidSolution() {
        var comm0 = new Commercial(0, 1, 0, 30, 2.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 40, 3.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 20, 5.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 50, 1.5, PricingType.FIXED);
        var comm4 = new Commercial(4, 5, 0, 25, 4.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3, comm4};

        var inv0 = new Inventory(0, 200, 8, 10);
        var inv1 = new Inventory(1, 150, 9, 10);
        var inventories = new Inventory[] {inv0, inv1};

        var suitability =
                new boolean[][] {
                    {true, true},
                    {true, true},
                    {true, true},
                    {true, true},
                    {true, true}
                };
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}}
                };

        var revenueMatrix = buildUniformRevenueMatrix(5, 2, 201, 50.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var config = new BeeColonyConfig(new RunInfo("test", 42), 5, 0.02, 0.95, 10, 2);
        var algorithm = new BeeColonyAlgorithm(config, new Random(42));

        var result = algorithm.run(problem);

        assertNotNull(result);
        assertNotNull(result.getBestSolution());
        assertTrue(result.getBestSolution().getTotalRevenue() > 0);
        assertFalse(result.getCheckPoints().isEmpty());
        assertNotNull(result.getAdditionalInformation());
    }

    @Test
    void solutionPassesFeasibilityCheck() {
        var comm0 = new Commercial(0, 1, 0, 20, 3.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 30, 2.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 25, 4.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};

        var inv0 = new Inventory(0, 100, 0, 10);
        var inv1 = new Inventory(1, 100, 0, 10);
        var inventories = new Inventory[] {inv0, inv1};

        var suitability =
                new boolean[][] {
                    {true, true},
                    {true, true},
                    {true, true}
                };
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}}
                };

        var revenueMatrix = buildUniformRevenueMatrix(3, 2, 101, 60.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var config = new BeeColonyConfig(new RunInfo("test", 42), 3, 0.02, 0.95, 10, 2);
        var algorithm = new BeeColonyAlgorithm(config, new Random(42));

        var result = algorithm.run(problem);

        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result.getBestSolution()));
    }

    @Test
    void solutionPassesFeasibilityCheckWithLTypeAttention() {
        var comm0 = new Commercial(0, 1, 0, 20, 3.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 30, 2.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 25, 4.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};

        var inv0 = new Inventory(0, 100, 0, 10);
        var inventories = new Inventory[] {inv0};

        var suitability = new boolean[][] {{true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}}, {{AttentionType.F1, AttentionType.L1}}, {{AttentionType.N}}
                };

        var revenueMatrix = buildUniformRevenueMatrix(3, 1, 101, 60.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var config = new BeeColonyConfig(new RunInfo("test", 42), 3, 0.02, 0.95, 10, 2);
        var algorithm = new BeeColonyAlgorithm(config, new Random(42));

        var result = algorithm.run(problem);

        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result.getBestSolution()));
    }

    private double[][][] buildUniformRevenueMatrix(
            int numComms, int numInvs, int timeSlots, double value) {
        var matrix = new double[numComms][numInvs][timeSlots];
        for (var commSlice : matrix) {
            for (var invSlice : commSlice) {
                Arrays.fill(invSlice, value);
            }
        }
        return matrix;
    }

    private Problem buildProblem(
            Commercial[] commercials,
            Inventory[] inventories,
            boolean[][] suitability,
            AttentionType[][][] attentionTypes,
            double[][][] revenueMatrix) {
        var hours = Arrays.stream(inventories).mapToInt(Inventory::getHour).distinct().toArray();
        var suitInvFor = new int[commercials.length][];
        for (int c = 0; c < commercials.length; c++) {
            var list = new ArrayList<Integer>();
            for (int i = 0; i < inventories.length; i++) {
                if (suitability[c][i]) {
                    list.add(i);
                }
            }
            suitInvFor[c] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        var suitCommFor = new int[inventories.length][];
        for (int i = 0; i < inventories.length; i++) {
            var list = new ArrayList<Integer>();
            for (int c = 0; c < commercials.length; c++) {
                if (suitability[c][i]) {
                    list.add(c);
                }
            }
            suitCommFor[i] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        return new Problem(
                commercials,
                inventories,
                hours,
                suitability,
                attentionTypes,
                suitInvFor,
                suitCommFor,
                new double[][][] {{{0.0}}},
                revenueMatrix);
    }
}
