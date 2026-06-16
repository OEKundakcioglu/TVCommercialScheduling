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
import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.heuristic.grasp.GraspSolution;

class OutOfPoolSwapMoveTest {

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
    void feasibleBasicSwap() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleNotSuitable() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {false}};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        suitability,
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleDurationExceeded() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 30, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 25, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        // inv0: [comm0(10), comm2(10)] = 20 <= 25
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}});
        // swap comm0(10) for comm1(30): total would be 40 > 25
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleDurationWhenNewCommShorter() {
        var comm0 = new Commercial(0, 1, 0, 20, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 20, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        // swap comm0(20) for comm1(10): shorter, always fits
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleHourlyLimit() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 720, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var sol = buildSolution(problem, new int[][] {{0}});
        var totalDurationOfHour = new int[] {715};
        var tweakedSolution =
                new GraspSolution(
                        sol.getSequences(),
                        sol.getStartTimes(),
                        sol.getRevenues(),
                        sol.getTotalRevenue(),
                        totalDurationOfHour,
                        sol.getTotalInvDuration(),
                        sol.getAssignedInvId(),
                        sol.getAssignedPos());
        // swap comm0(10) for comm1(20): delta=+10, hour total 715+10=725 > 720
        var move = new OutOfPoolSwapMove(problem, tweakedSolution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttention() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var attentionTypes = buildNAttentionTypes(3, 1);
        // comm1 requires F1 at inv0 — must be at position 0
        attentionTypes[1][0] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 1, inventories));
        // inv0: [comm0, comm2] → swap comm2 at position 1 for comm1(F1)
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}});
        var move = new OutOfPoolSwapMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroup() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        // inv0: [comm0(group=1), comm2(group=3)] → swap comm2 for comm1(group=1)
        // comm0(group=1) and comm1(group=1) would be adjacent → infeasible
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}});
        var move = new OutOfPoolSwapMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    // --- computeRevenueGain ---

    @Test
    void revenueGainWithConstantRevenueSameDuration() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var revenueMatrix = buildConstantRevenueMatrix(2, 1, inventories);
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        revenueMatrix);
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        // comm0 rev=100, comm1 rev=200 (constant), gain = 200-100 = 100
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertEquals(100.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithDifferentDurationsAndShift() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
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
        // inv0: [comm0(dur=10), comm2(dur=10)] → startTimes [0, 10]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}});
        // swap comm0(dur=10) for comm1(dur=20) at pos 0
        // comm1 rev at t=0: 200+0=200. comm0 rev at t=0: 100+0=100 → direct delta=+100
        // comm2 shifts from t=10 to t=20: rev(2,0,20)=320, rev(2,0,10)=310 → delta=+10
        // total = 110
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertEquals(110.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithShorterNewCommercial() {
        var comm0 = new Commercial(0, 1, 0, 20, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
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
        // inv0: [comm0(dur=20), comm2(dur=10)] → startTimes [0, 20]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}});
        // swap comm0(dur=20) for comm1(dur=10) at pos 0
        // comm1 rev at t=0: 200. comm0 rev at t=0: 100 → direct delta=+100
        // comm2 shifts from t=20 to t=10: rev(2,0,10)=310, rev(2,0,20)=320 → delta=-10
        // total = 90
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        assertEquals(90.0, move.calculateRevenueGain(), 1e-6);
    }

    // --- apply ---

    @Test
    void applyReplacesCommercial() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var result = new OutOfPoolSwapMove(problem, solution, 0, 0, 1).apply();
        assertArrayEquals(new int[] {1}, result.getSequences()[0]);
    }

    @Test
    void applyRebuildsStartTimesWithDifferentDurations() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        // inv0: [comm0(10), comm2(10)] → startTimes [0, 10]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}});
        var result = new OutOfPoolSwapMove(problem, solution, 0, 0, 1).apply();
        assertArrayEquals(new int[] {1, 2}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0, 20}, result.getStartTimes()[0]);
    }

    @Test
    void applyDoesNotMutateOriginal() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var origSeq = solution.getSequences()[0].clone();
        final var origTotalRevenue = solution.getTotalRevenue();
        new OutOfPoolSwapMove(problem, solution, 0, 0, 1).apply();
        assertArrayEquals(origSeq, solution.getSequences()[0]);
        assertEquals(origTotalRevenue, solution.getTotalRevenue(), 1e-6);
    }

    @Test
    void applyUpdatesTotalInvDuration() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var result = new OutOfPoolSwapMove(problem, solution, 0, 0, 1).apply();
        assertEquals(20, result.getTotalInvDuration()[0]);
    }

    @Test
    void applyUpdatesTotalDurationOfHour() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var result = new OutOfPoolSwapMove(problem, solution, 0, 0, 1).apply();
        assertEquals(20, result.getTotalDurationOfHour()[0]);
    }

    @Test
    void applyPassesFeasibilityCheck() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void applyTotalRevenueMatchesGain() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}});
        var move = new OutOfPoolSwapMove(problem, solution, 0, 0, 1);
        var result = move.apply();
        assertEquals(
                solution.getTotalRevenue() + move.calculateRevenueGain(),
                result.getTotalRevenue(),
                1e-6);
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
        var possibleHours = new int[] {0, 1, 2};
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

            var assignedComms = new HashSet<Integer>();
            for (var seq : solution.getSequences()) {
                for (var commId : seq) {
                    assignedComms.add(commId);
                }
            }

            var unassignedComms = new ArrayList<Integer>();
            for (int c = 0; c < problem.getCommercials().length; c++) {
                if (!assignedComms.contains(c)) {
                    unassignedComms.add(c);
                }
            }

            for (int attempt = 0; attempt < 10; attempt++) {
                if (unassignedComms.isEmpty()) {
                    break;
                }

                var candidateInvs = new ArrayList<Integer>();
                for (int inv = 0; inv < problem.getInventories().length; inv++) {
                    if (solution.getSequences()[inv].length >= 1) {
                        candidateInvs.add(inv);
                    }
                }
                if (candidateInvs.isEmpty()) {
                    break;
                }

                var invId = candidateInvs.get(rng.nextInt(candidateInvs.size()));
                var pos = rng.nextInt(solution.getSequences()[invId].length);
                var newCommId = unassignedComms.get(rng.nextInt(unassignedComms.size()));

                var move = new OutOfPoolSwapMove(problem, solution, invId, pos, newCommId);
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
