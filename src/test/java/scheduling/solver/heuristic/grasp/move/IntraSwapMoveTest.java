package scheduling.solver.heuristic.grasp.move;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.heuristic.grasp.GraspSolution;

class IntraSwapMoveTest {

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

    private GraspSolution buildSolution(Problem problem, int[][] sequences) {
        var numInv = problem.getInventories().length;
        var numComm = problem.getCommercials().length;
        var startTimes = new int[numInv][];
        var revenues = new double[numInv][];
        var totalRevenue = 0.0;
        var maxHour = Arrays.stream(problem.getHours()).max().orElse(0);
        var totalDurationOfHour = new int[maxHour + 1];
        var totalInvDuration = new int[numInv];
        var assignedInvId = new int[numComm];
        var assignedPos = new int[numComm];
        Arrays.fill(assignedInvId, -1);
        Arrays.fill(assignedPos, -1);

        for (int inv = 0; inv < numInv; inv++) {
            var seq = sequences[inv];
            startTimes[inv] = new int[seq.length];
            revenues[inv] = new double[seq.length];
            var currentTime = 0;
            for (int pos = 0; pos < seq.length; pos++) {
                startTimes[inv][pos] = currentTime;
                revenues[inv][pos] = problem.getRevenue(seq[pos], inv, currentTime);
                totalRevenue += revenues[inv][pos];
                currentTime += problem.getCommercial(seq[pos]).getDuration();
                assignedInvId[seq[pos]] = inv;
                assignedPos[seq[pos]] = pos;
            }
            totalInvDuration[inv] = currentTime;
            var invHour = problem.getInventory(inv).getHour();
            totalDurationOfHour[invHour] += currentTime;
        }
        return new GraspSolution(
                sequences,
                startTimes,
                revenues,
                totalRevenue,
                totalDurationOfHour,
                totalInvDuration,
                assignedInvId,
                assignedPos);
    }

    private GraspSolution buildCheckedSolution(Problem problem, int[][] sequences) {
        var solution = buildSolution(problem, sequences);
        FeasibilityCheck.check(problem, solution);
        return solution;
    }

    private double[][][] buildConstantRevenueMatrix(
            int numComm, int numInv, Inventory[] inventories) {
        var matrix = new double[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                matrix[c][i] = new double[inventories[i].getDuration() + 1];
                Arrays.fill(matrix[c][i], 100.0 * (c + 1));
            }
        }
        return matrix;
    }

    private AttentionType[][][] buildNAttentionTypes(int numComm, int numInv) {
        var types = new AttentionType[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                types[c][i] = new AttentionType[] {AttentionType.N};
            }
        }
        return types;
    }

    private boolean[][] buildAllSuitable(int numComm, int numInv) {
        var suit = new boolean[numComm][numInv];
        for (int c = 0; c < numComm; c++) {
            Arrays.fill(suit[c], true);
        }
        return suit;
    }

    // --- checkFeasibility ---

    @Test
    void feasibleSwapTwoElements() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleSwapNonAdjacent() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 2);
        assertTrue(move.checkFeasibility());
    }

    // --- attention feasibility ---

    @Test
    void infeasibleAttentionComm1AtNewPosition() {
        // comm0 has F1 (must be at pos 0), swap it to pos 1 → infeasible
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var attentionTypes = buildNAttentionTypes(2, 1);
        attentionTypes[0][0] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        // swap pos0 and pos1 → comm0(F1) goes to pos1
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionComm2AtNewPosition() {
        // comm1 has L1 (must be last), swap it from pos 1 (last) to pos 0 → infeasible
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var attentionTypes = buildNAttentionTypes(2, 1);
        attentionTypes[1][0] = new AttentionType[] {AttentionType.L1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(2, 1, inventories));
        // sequence [0, 1] — comm1(L1) is at pos 1 (last, satisfied)
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        // swap pos0 and pos1 → comm1(L1) goes to pos 0 (not last → infeasible)
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleAttentionAfterSwap() {
        // comm0 has L1, comm1 has F1. Sequence [1, 0] — both violated.
        // Swap pos0 and pos1 → [0, 1] → comm0(L1) at pos1 (last), comm1(F1) not involved
        // Wait, this doesn't work because the initial solution would be infeasible.
        // Let's do: comm0(F12), comm1(N). Sequence [0, 1]. Swap → [1, 0].
        // comm0(F12) at pos1 — F12 requires pos<=1, so pos1 is ok. Feasible.
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var attentionTypes = buildNAttentionTypes(2, 1);
        attentionTypes[0][0] = new AttentionType[] {AttentionType.F12};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        // swap → comm0(F12) at pos 1, F12 requires pos<=1 ✓
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertTrue(move.checkFeasibility());
    }

    // --- group feasibility ---

    @Test
    void infeasibleGroupAfterSwapLeftNeighbor() {
        // [comm0(g=1), comm1(g=2), comm2(g=1)], swap pos1 and pos2
        // → [comm0(g=1), comm2(g=1), comm1(g=2)] — comm0 and comm2 adjacent, same group
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 1, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new IntraSwapMove(problem, solution, 0, 1, 2);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAfterSwapRightNeighbor() {
        // [comm0(g=1), comm1(g=2), comm2(g=1)], swap pos0 and pos1
        // → [comm1(g=2), comm0(g=1), comm2(g=1)] — comm0 and comm2 adjacent, same group
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 1, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleGroupAfterNonAdjacentSwap() {
        // [comm0(g=1), comm1(g=2), comm2(g=3)], swap pos0 and pos2
        // → [comm2(g=3), comm1(g=2), comm0(g=1)] — all different neighbors
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 2);
        assertTrue(move.checkFeasibility());
    }

    // --- calculateRevenueGain ---

    @Test
    void revenueGainZeroWithConstantRevenue() {
        // With constant revenue per commercial, swapping doesn't change revenue
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithTimeVaryingRevenueSameDuration() {
        // Same durations → no shift for in-between, only the two swapped change
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var revenueMatrix = new double[2][1][];
        for (int c = 0; c < 2; c++) {
            revenueMatrix[c][0] = new double[121];
            for (int t = 0; t < 121; t++) {
                revenueMatrix[c][0][t] = (c + 1) * 100.0 + t * 2.0;
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        revenueMatrix);
        // [comm0, comm1] → startTimes [0, 10]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        // swap → [comm1, comm0] → startTimes [0, 10] (same durations)
        // comm1 at t=0: 200.0, was at t=10: 220.0 → delta = -20.0
        // comm0 at t=10: 120.0, was at t=0: 100.0 → delta = +20.0
        // total gain = 0.0
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithDifferentDurationsAndShift() {
        // Different durations → in-between commercials shift
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 5, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 20, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var revenueMatrix = new double[3][1][];
        for (int c = 0; c < 3; c++) {
            revenueMatrix[c][0] = new double[121];
            for (int t = 0; t < 121; t++) {
                revenueMatrix[c][0][t] = (c + 1) * 100.0 + t * 1.0;
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        revenueMatrix);
        // [comm0(dur=10), comm1(dur=5), comm2(dur=20)] → startTimes [0, 10, 15]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        // swap pos0 and pos2 → [comm2(dur=20), comm1(dur=5), comm0(dur=10)]
        // shift = comm2.dur - comm0.dur = 20 - 10 = 10
        // comm2 at pos0, t=0: 300.0, was at t=15: 315.0 → delta = -15.0
        // comm1 at pos1, t=0+20=20: 220.0, was at t=10: 210.0 → delta = +10.0
        // comm0 at pos2, t=15+10=25: 125.0, was at t=0: 100.0 → delta = +25.0
        // total = -15.0 + 10.0 + 25.0 = 20.0
        var move = new IntraSwapMove(problem, solution, 0, 0, 2);
        assertEquals(20.0, move.calculateRevenueGain(), 1e-6);
    }

    // --- apply ---

    @Test
    void applySwapsTwoElements() {
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        Arrays.fill(revenueMatrix[0][0], 100.0);
        revenueMatrix[1][0] = new double[121];
        Arrays.fill(revenueMatrix[1][0], 200.0);
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        revenueMatrix);
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        var result = move.apply();
        assertArrayEquals(new int[] {1, 0}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0, 10}, result.getStartTimes()[0]);
        assertEquals(200.0, result.getRevenues()[0][0], 1e-6);
        assertEquals(100.0, result.getRevenues()[0][1], 1e-6);
        assertEquals(
                solution.getTotalRevenue() + move.calculateRevenueGain(),
                result.getTotalRevenue(),
                1e-6);
    }

    @Test
    void applyWithDifferentDurationsRebuildsStartTimes() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 5, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 20, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        // [comm0(10), comm1(5), comm2(20)] → startTimes [0, 10, 15]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        // swap pos0 and pos2 → [comm2(20), comm1(5), comm0(10)]
        var result = new IntraSwapMove(problem, solution, 0, 0, 2).apply();
        assertArrayEquals(new int[] {2, 1, 0}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0, 20, 25}, result.getStartTimes()[0]);
    }

    @Test
    void applyDoesNotMutateOriginal() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var originalSeq = solution.getSequences()[0].clone();
        var originalStartTimes = solution.getStartTimes()[0].clone();

        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        final var originalRevenues = solution.getRevenues()[0].clone();
        final var originalTotalRevenue = solution.getTotalRevenue();
        final var originalTotalDurationOfHour = solution.getTotalDurationOfHour().clone();
        move.apply();

        assertArrayEquals(originalSeq, solution.getSequences()[0]);
        assertArrayEquals(originalStartTimes, solution.getStartTimes()[0]);
        for (int i = 0; i < originalRevenues.length; i++) {
            assertEquals(originalRevenues[i], solution.getRevenues()[0][i], 1e-6);
        }
        assertEquals(originalTotalRevenue, solution.getTotalRevenue(), 1e-6);
        assertArrayEquals(originalTotalDurationOfHour, solution.getTotalDurationOfHour());
    }

    @Test
    void applyDoesNotChangeTotalDurationOfHour() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var result = new IntraSwapMove(problem, solution, 0, 0, 1).apply();
        assertArrayEquals(solution.getTotalDurationOfHour(), result.getTotalDurationOfHour());
    }

    @Test
    void applyPassesFeasibilityCheck() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var move = new IntraSwapMove(problem, solution, 0, 0, 1);
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void applyDoesNotAffectOtherInventories() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inv1 = new Inventory(1, 120, 2, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 2),
                        buildNAttentionTypes(3, 2),
                        buildConstantRevenueMatrix(3, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}, {2}});
        var result = new IntraSwapMove(problem, solution, 0, 0, 1).apply();
        assertArrayEquals(new int[] {2}, result.getSequences()[1]);
        assertArrayEquals(solution.getStartTimes()[1], result.getStartTimes()[1]);
    }

    // --- brute force ---

    private Problem buildRandomProblem(Random rng) {
        var numComm = 30;
        var numInv = 8;
        var commercials = new Commercial[numComm];
        for (int c = 0; c < numComm; c++) {
            var duration = 5 + rng.nextInt(26);
            var group = 1 + rng.nextInt(6);
            var price = 10.0 + rng.nextInt(91);
            commercials[c] = new Commercial(c, group, 0, duration, price, PricingType.FIXED);
        }
        var inventories = new Inventory[numInv];
        var possibleHours = new int[] {1, 2, 3};
        for (int i = 0; i < numInv; i++) {
            var duration = 60 + rng.nextInt(241);
            var hour = possibleHours[rng.nextInt(3)];
            inventories[i] = new Inventory(i, duration, hour, 20);
        }
        var suitability = new boolean[numComm][numInv];
        for (int c = 0; c < numComm; c++) {
            var hasSuitable = false;
            for (int i = 0; i < numInv; i++) {
                suitability[c][i] = rng.nextDouble() < 0.6;
                if (suitability[c][i]) {
                    hasSuitable = true;
                }
            }
            if (!hasSuitable) {
                suitability[c][rng.nextInt(numInv)] = true;
            }
        }
        var attentionPool =
                new AttentionType[] {
                    AttentionType.N,
                    AttentionType.N,
                    AttentionType.N,
                    AttentionType.F1,
                    AttentionType.F2,
                    AttentionType.F3,
                    AttentionType.F12,
                    AttentionType.F123,
                    AttentionType.L1,
                    AttentionType.L2,
                    AttentionType.L12,
                    AttentionType.L123
                };
        var attentionTypes = new AttentionType[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                attentionTypes[c][i] =
                        new AttentionType[] {attentionPool[rng.nextInt(attentionPool.length)]};
            }
        }
        var revenueMatrix = new double[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                revenueMatrix[c][i] = new double[inventories[i].getDuration() + 1];
                for (int t = 0; t <= inventories[i].getDuration(); t++) {
                    revenueMatrix[c][i][t] = (c + 1) * 50.0 + t * 0.5;
                }
            }
        }
        return buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
    }

    private GraspSolution buildRandomFeasibleSolution(Problem problem, Random rng) {
        var numComm = problem.getCommercials().length;
        var numInv = problem.getInventories().length;
        var seqLists = new ArrayList<ArrayList<Integer>>();
        for (int i = 0; i < numInv; i++) {
            seqLists.add(new ArrayList<>());
        }
        var commIds = new ArrayList<Integer>();
        for (int c = 0; c < numComm; c++) {
            commIds.add(c);
        }
        Collections.shuffle(commIds, rng);
        for (var commId : commIds) {
            var suitableInvs = problem.getSuitableInventories(commId);
            if (suitableInvs.length == 0) {
                continue;
            }
            var invId = suitableInvs[rng.nextInt(suitableInvs.length)];
            var pos = rng.nextInt(seqLists.get(invId).size() + 1);
            seqLists.get(invId).add(pos, commId);
            var totalDuration = 0;
            for (var commIdInSeq : seqLists.get(invId)) {
                totalDuration += problem.getCommercial(commIdInSeq).getDuration();
            }
            if (totalDuration > problem.getInventory(invId).getDuration()) {
                seqLists.get(invId).remove(pos);
                continue;
            }
            var sequences = new int[numInv][];
            for (int i = 0; i < numInv; i++) {
                sequences[i] = seqLists.get(i).stream().mapToInt(Integer::intValue).toArray();
            }
            var candidate = buildSolution(problem, sequences);
            try {
                FeasibilityCheck.check(problem, candidate);
            } catch (IllegalStateException e) {
                seqLists.get(invId).remove(pos);
            }
        }
        var sequences = new int[numInv][];
        for (int i = 0; i < numInv; i++) {
            sequences[i] = seqLists.get(i).stream().mapToInt(Integer::intValue).toArray();
        }
        return buildSolution(problem, sequences);
    }

    @Test
    void bruteForceValidation() {
        var rng = new Random(42);
        var feasibleCount = 0;
        var infeasibleCount = 0;

        for (int trial = 0; trial < 1000; trial++) {
            var problem = buildRandomProblem(rng);
            var solution = buildRandomFeasibleSolution(problem, rng);

            for (int attempt = 0; attempt < 10; attempt++) {
                // Pick a random inventory with at least 2 commercials
                var candidateInvs = new ArrayList<Integer>();
                for (int inv = 0; inv < problem.getInventories().length; inv++) {
                    if (solution.getSequences()[inv].length >= 2) {
                        candidateInvs.add(inv);
                    }
                }
                if (candidateInvs.isEmpty()) {
                    break;
                }
                var invId = candidateInvs.get(rng.nextInt(candidateInvs.size()));
                var seqLen = solution.getSequences()[invId].length;
                var p1 = rng.nextInt(seqLen);
                var p2 = rng.nextInt(seqLen);
                if (p1 == p2) {
                    continue;
                }
                var pos1 = Math.min(p1, p2);
                var pos2 = Math.max(p1, p2);

                var move = new IntraSwapMove(problem, solution, invId, pos1, pos2);
                var feasible = move.checkFeasibility();
                var applied = move.apply();
                var gain = move.calculateRevenueGain();

                assertEquals(
                        applied.getTotalRevenue() - solution.getTotalRevenue(),
                        gain,
                        1e-6,
                        "Revenue gain mismatch at trial=" + trial + " attempt=" + attempt);

                if (feasible) {
                    feasibleCount++;
                    assertDoesNotThrow(
                            () -> FeasibilityCheck.check(problem, applied),
                            "checkFeasibility=true but FeasibilityCheck failed at trial=" + trial);
                } else {
                    infeasibleCount++;
                    assertThrows(
                            IllegalStateException.class,
                            () -> FeasibilityCheck.check(problem, applied),
                            "checkFeasibility=false but FeasibilityCheck passed at trial=" + trial);
                }
            }
        }

        assertTrue(feasibleCount > 0, "No feasible moves were tested");
        assertTrue(infeasibleCount > 0, "No infeasible moves were tested");
    }
}
