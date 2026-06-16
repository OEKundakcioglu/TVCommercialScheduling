package scheduling.solver.heuristic.beecolony;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.heuristic.beecolony.vrp.VrpConverter;
import scheduling.solver.heuristic.grasp.GraspSolution;

class SolutionStringConverterTest {

    @Test
    void convertsGraspSolutionToSolutionString() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 5.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 5, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 25, 8, 10);
        var inv1 = new Inventory(1, 20, 9, 10);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inventories = new Inventory[] {inv0, inv1};
        var suitability = new boolean[][] {{true, true}, {true, true}, {true, true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}}
                };
        var revenueMatrix = new double[][][] {{{0.0}, {0.0}}, {{0.0}, {0.0}}, {{0.0}, {0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var depotId = vrp.getDepot().id();

        var graspSolution =
                new GraspSolution(
                        new int[][] {{0, 2}, {1}},
                        new int[][] {{0, 10}, {0}},
                        new double[][] {{0, 0}, {0}},
                        0.0,
                        new int[] {25, 20},
                        new int[] {25, 20},
                        new int[] {0, 1, 0},
                        new int[] {0, 0, 1});

        var result = SolutionStringConverter.toSolutionString(graspSolution, vrp);

        assertArrayEquals(new int[] {0, 2, depotId, 1}, result);
    }

    @Test
    void convertsSolutionStringToSolution() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 3.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var inv1 = new Inventory(1, 200, 9, 10);
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

        var beeSolution = new BeeColonySolution(new int[] {0, depotId, 1}, 110.0);

        var solution = SolutionStringConverter.toSolution(beeSolution, vrp);

        assertEquals(110.0, solution.getTotalRevenue());
        assertEquals(List.of(comm0), solution.getAssignments().get(inv0));
        assertEquals(List.of(comm1), solution.getAssignments().get(inv1));
    }

    @Test
    void toSolutionSkipsInfeasibleCustomers() {
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

        var beeSolution = new BeeColonySolution(new int[] {0, 1}, 50.0);

        var solution = SolutionStringConverter.toSolution(beeSolution, vrp);

        assertEquals(50.0, solution.getTotalRevenue());
        assertEquals(List.of(comm0), solution.getAssignments().get(inv0));
    }

    @Test
    void handlesEmptyInventories() {
        var comm0 = new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var inv1 = new Inventory(1, 100, 9, 10);
        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0, inv1};
        var suitability = new boolean[][] {{true, false}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}, {AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{50.0}, {0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var vrp = VrpConverter.convert(problem);
        var depotId = vrp.getDepot().id();

        var beeSolution = new BeeColonySolution(new int[] {0, depotId}, 50.0);

        var solution = SolutionStringConverter.toSolution(beeSolution, vrp);

        assertEquals(50.0, solution.getTotalRevenue());
        assertEquals(List.of(comm0), solution.getAssignments().get(inv0));
        assertEquals(List.of(), solution.getAssignments().get(inv1));
    }

    @Test
    void toSolutionSkipsCustomersExceedingCommercialCountLimit() {
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
        var beeSolution = new BeeColonySolution(new int[] {0, 1}, 75.0);

        var solution = SolutionStringConverter.toSolution(beeSolution, vrp);

        assertEquals(75.0, solution.getTotalRevenue());
        assertEquals(List.of(comm0), solution.getAssignments().get(inv0));
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, solution));
    }

    @Test
    void rejectsSolutionThatOnlyFailsFinalLTypeAttention() {
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
        var beeSolution = new BeeColonySolution(new int[] {0, 1, 2}, 150.0);

        assertThrows(
                IllegalStateException.class,
                () -> SolutionStringConverter.toSolution(beeSolution, vrp));
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
