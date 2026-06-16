package scheduling.solver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.heuristic.grasp.GraspSolution;

class FeasibilityCheckTest {

    private Problem buildSimpleProblem(
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

    private Problem twoCommOneProblem() {
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[1][0] = new double[121];
        for (int t = 0; t < 121; t++) {
            revenueMatrix[0][0][t] = 1000.0;
            revenueMatrix[1][0][t] = 4000.0;
        }
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        return buildSimpleProblem(
                new Commercial[] {comm0, comm1},
                new Inventory[] {inv0},
                new boolean[][] {{true}, {true}},
                new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                revenueMatrix);
    }

    @Test
    void feasibleSolutionPassesAllChecks() {
        var problem = twoCommOneProblem();
        var inv0 = problem.getInventory(0);
        var comm0 = problem.getCommercial(0);
        var comm1 = problem.getCommercial(1);
        var solution = new Solution(Map.of(inv0, List.of(comm0, comm1)), 5000.0);
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, solution));
    }

    @Test
    void detectsUnsuitableAssignment() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var revenueMatrix = new double[1][1][];
        revenueMatrix[0][0] = new double[121];
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0},
                        new Inventory[] {inv0},
                        new boolean[][] {{false}},
                        new AttentionType[][][] {{{AttentionType.N}}},
                        revenueMatrix);
        var solution = new Solution(Map.of(inv0, List.of(comm0)), 0.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("not suitable"));
    }

    @Test
    void detectsAttentionViolation() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[1][0] = new double[121];
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0},
                        new boolean[][] {{true}, {true}},
                        new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.F1}}},
                        revenueMatrix);
        var solution = new Solution(Map.of(inv0, List.of(comm0, comm1)), 0.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("attention"));
    }

    @Test
    void detectsGroupViolation() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 20, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[1][0] = new double[121];
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0},
                        new boolean[][] {{true}, {true}},
                        new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                        revenueMatrix);
        var solution = new Solution(Map.of(inv0, List.of(comm0, comm1)), 0.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("group"));
    }

    @Test
    void detectsDurationViolation() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 15, 1, 5);
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[16];
        revenueMatrix[1][0] = new double[16];
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0},
                        new boolean[][] {{true}, {true}},
                        new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                        revenueMatrix);
        var solution = new Solution(Map.of(inv0, List.of(comm0, comm1)), 0.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("duration"));
    }

    @Test
    void detectsCommercialCountViolation() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 1); // maxCommercialCount = 1
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[1][0] = new double[121];
        for (int t = 0; t < 121; t++) {
            revenueMatrix[0][0][t] = 1000.0;
            revenueMatrix[1][0][t] = 2000.0;
        }
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0},
                        new boolean[][] {{true}, {true}},
                        new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                        revenueMatrix);
        var solution = new Solution(Map.of(inv0, List.of(comm0, comm1)), 3000.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("max allowed"));
    }

    @Test
    void detectsHourlyLimitViolation() {
        // Two inventories in same hour, each with commercials summing to 400s => 800 > 720
        var revenueMatrix = new double[2][2][];
        revenueMatrix[0][0] = new double[501];
        revenueMatrix[0][1] = new double[501];
        revenueMatrix[1][0] = new double[501];
        revenueMatrix[1][1] = new double[501];
        var comm0 = new Commercial(0, 1, 0, 400, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 400, 1.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 500, 1, 5);
        var inv1 = new Inventory(1, 500, 1, 5); // same hour
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0, inv1},
                        new boolean[][] {{true, true}, {true, true}},
                        new AttentionType[][][] {
                            {{AttentionType.N}, {AttentionType.N}},
                            {{AttentionType.N}, {AttentionType.N}}
                        },
                        revenueMatrix);
        var solution = new Solution(Map.of(inv0, List.of(comm0), inv1, List.of(comm1)), 0.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("hourly limit"));
    }

    @Test
    void detectsDuplicateCommercialAssignment() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var inv1 = new Inventory(1, 120, 2, 5);
        var revenueMatrix = new double[1][2][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[0][1] = new double[121];
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0},
                        new Inventory[] {inv0, inv1},
                        new boolean[][] {{true, true}},
                        new AttentionType[][][] {{{AttentionType.N}, {AttentionType.N}}},
                        revenueMatrix);
        // comm0 assigned to both inventories
        var solution = new Solution(Map.of(inv0, List.of(comm0), inv1, List.of(comm0)), 0.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("multiple inventories"));
    }

    @Test
    void detectsRevenueMismatch() {
        var problem = twoCommOneProblem();
        var inv0 = problem.getInventory(0);
        var comm0 = problem.getCommercial(0);
        var comm1 = problem.getCommercial(1);
        // Correct revenue is 5000.0, pass wrong value
        var solution = new Solution(Map.of(inv0, List.of(comm0, comm1)), 9999.0);
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, solution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("revenue"));
    }

    @Test
    void graspSolutionOverloadDelegatesToSolutionCheck() {
        var problem = twoCommOneProblem();
        // Build a feasible GraspSolution
        var sequences = new int[][] {{0, 1}};
        var startTimes = new int[][] {{0, 10}};
        var revenues = new double[][] {{1000.0, 4000.0}};
        var totalDurationOfHour = new int[] {30};
        var totalInvDuration = new int[] {30};
        var graspSolution =
                new GraspSolution(
                        sequences,
                        startTimes,
                        revenues,
                        5000.0,
                        totalDurationOfHour,
                        totalInvDuration,
                        new int[] {0, 0},
                        new int[] {0, 1});
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, graspSolution));
    }

    @Test
    void graspSolutionOverloadDetectsViolation() {
        // Same group adjacent — should fail group check
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 20, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[1][0] = new double[121];
        var problem =
                buildSimpleProblem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0},
                        new boolean[][] {{true}, {true}},
                        new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                        revenueMatrix);
        var sequences = new int[][] {{0, 1}};
        var startTimes = new int[][] {{0, 10}};
        var revenues = new double[][] {{0.0, 0.0}};
        var totalDurationOfHour = new int[] {30};
        var totalInvDuration = new int[] {30};
        var graspSolution =
                new GraspSolution(
                        sequences,
                        startTimes,
                        revenues,
                        0.0,
                        totalDurationOfHour,
                        totalInvDuration,
                        new int[] {0, 0},
                        new int[] {0, 1});
        var ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> FeasibilityCheck.check(problem, graspSolution));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("group"));
    }
}
