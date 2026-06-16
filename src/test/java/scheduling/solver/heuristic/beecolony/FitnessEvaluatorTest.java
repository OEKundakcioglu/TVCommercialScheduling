package scheduling.solver.heuristic.beecolony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.heuristic.beecolony.vrp.VrpConverter;

class FitnessEvaluatorTest {

    @Test
    void singleCustomerSingleVehicle() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{50.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0});

        assertEquals(50.0, fitness);
    }

    @Test
    void twoCustomersTwoVehicles() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 3.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var inv1 = new Inventory(1, 100, 9, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0, inv1};
        var suitability = new boolean[][] {{true, true}, {true, true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}}
                };
        var revenueMatrix = new double[][][] {{{50.0}, {0.0}}, {{0.0}, {60.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var depotId = vrp.getDepot().id();
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, depotId, 1});

        assertEquals(110.0, fitness);
    }

    @Test
    void skipsInfeasibleCustomer() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {false}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{50.0}}, {{60.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1});

        assertEquals(50.0, fitness);
    }

    @Test
    void skipsCustomerExceedingTimeLimit() {
        var comm0 = new Commercial(0, 1, 0, 60, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 60, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = buildUniformRevenueMatrix(2, 1, 101, 300.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1});

        assertEquals(300.0, fitness);
    }

    @Test
    void skipsCustomerExceedingHourlyLimit() {
        var comm0 = new Commercial(0, 1, 0, 200, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 200, 5.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 200, 5.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 200, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 1000, 8, 10);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}},
                    {{AttentionType.N}},
                    {{AttentionType.N}},
                    {{AttentionType.N}}
                };
        var revenueMatrix = buildUniformRevenueMatrix(4, 1, 1001, 1000.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1, 2, 3});

        assertEquals(3000.0, fitness);
    }

    @Test
    void skipsCustomerExceedingCommercialCountLimit() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 1);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = buildUniformRevenueMatrix(2, 1, 101, 75.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1});

        assertEquals(75.0, fitness);
    }

    @Test
    void emptyStringReturnsZero() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{50.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[0]);

        assertEquals(0.0, fitness);
    }

    @Test
    void skipsCustomerWithSameGroupAdjacency() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = buildUniformRevenueMatrix(2, 1, 101, 50.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1});

        assertEquals(50.0, fitness);
    }

    @Test
    void penalizesSolutionThatOnlyFailsFinalLTypeAttention() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 5.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 5.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 100, 8, 10);
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}}, {{AttentionType.F1, AttentionType.L1}}, {{AttentionType.N}}
                };
        var revenueMatrix = buildUniformRevenueMatrix(3, 1, 101, 50.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1, 2});

        assertTrue(fitness < 1.0);
    }

    @Test
    void penalizesSolutionThatOnlyFailsFinalL123Attention() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 5.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 5.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 10, 5.0, PricingType.FIXED);
        var comm4 = new Commercial(4, 5, 0, 10, 5.0, PricingType.FIXED);
        var comm5 = new Commercial(5, 6, 0, 10, 5.0, PricingType.FIXED);
        var comm6 = new Commercial(6, 7, 0, 10, 5.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3, comm4, comm5, comm6};
        var inv0 = new Inventory(0, 100, 8, 10);
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}, {true}, {true}, {true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}},
                    {{AttentionType.N}},
                    {{AttentionType.N}},
                    {{AttentionType.F123, AttentionType.L123}},
                    {{AttentionType.N}},
                    {{AttentionType.N}},
                    {{AttentionType.N}}
                };
        var revenueMatrix = buildUniformRevenueMatrix(7, 1, 101, 50.0);
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrp);

        var fitness = evaluator.evaluate(new int[] {0, 1, 2, 3, 4, 5, 6});

        assertTrue(fitness < 1.0);
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
