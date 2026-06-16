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

class InterSwapMoveTest {

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
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleDurationExceeded() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 15, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleHourlyLimit() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 720, 0, 5);
        var inv1 = new Inventory(1, 720, 1, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var sequences = new int[][] {{0}, {1}};
        var sol = buildSolution(problem, sequences);
        var totalDurationOfHour = new int[] {715, 20};
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
        var move = new InterSwapMove(problem, tweakedSolution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleHourlyLimitSameHour() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionComm1AtNewPosition() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var attentionTypes = buildNAttentionTypes(3, 2);
        attentionTypes[0][1] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 2),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {2, 1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionComm2AtNewPosition() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var attentionTypes = buildNAttentionTypes(3, 2);
        attentionTypes[1][0] = new AttentionType[] {AttentionType.L1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 2),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAfterSwap() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 2),
                        buildNAttentionTypes(3, 2),
                        buildConstantRevenueMatrix(3, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 1, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleGroupSameGroupSwap() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertTrue(move.checkFeasibility());
    }

    // --- calculateRevenueGain ---

    @Test
    void revenueGainZeroWithConstantRevenueSameDuration() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithTimeVaryingRevenue() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var revenueMatrix = new double[2][2][];
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < 2; i++) {
                revenueMatrix[c][i] = new double[121];
                for (int t = 0; t < 121; t++) {
                    revenueMatrix[c][i][t] = (c + 1) * 100.0 + (i + 1) * 10.0 + t;
                }
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        revenueMatrix);
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        // comm0: was rev(0,0,0)=110, now rev(0,1,0)=120 → +10
        // comm1: was rev(1,1,0)=220, now rev(1,0,0)=210 → -10
        // total gain = 0
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithDifferentDurationsAndShift() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 10, 400.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var revenueMatrix = new double[4][2][];
        for (int c = 0; c < 4; c++) {
            for (int i = 0; i < 2; i++) {
                revenueMatrix[c][i] = new double[121];
                for (int t = 0; t < 121; t++) {
                    revenueMatrix[c][i][t] = (c + 1) * 100.0 + t * 1.0;
                }
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 2),
                        buildNAttentionTypes(4, 2),
                        revenueMatrix);
        // inv0: [comm0(dur=10), comm2(dur=10)] → startTimes [0, 10]
        // inv1: [comm1(dur=20), comm3(dur=10)] → startTimes [0, 20]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}, {1, 3}});
        // comm0: was rev(0,0,0)=100, now rev(0,1,0)=100 → delta=0
        // comm1: was rev(1,1,0)=200, now rev(1,0,0)=200 → delta=0
        // comm2 in inv0: was at t=10 rev=310, now at t=20 rev=320 → delta=+10
        // comm3 in inv1: was at t=20 rev=420, now at t=10 rev=410 → delta=-10
        // total gain = 0
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    // --- apply ---

    @Test
    void applySwapsCommercials() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var result = new InterSwapMove(problem, solution, 0, 0, 1, 0).apply();
        assertArrayEquals(new int[] {1}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0}, result.getSequences()[1]);
    }

    @Test
    void applyRebuildsStartTimesWithDifferentDurations() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 10, 400.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 2),
                        buildNAttentionTypes(4, 2),
                        buildConstantRevenueMatrix(4, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 2}, {1, 3}});
        var result = new InterSwapMove(problem, solution, 0, 0, 1, 0).apply();
        assertArrayEquals(new int[] {1, 2}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0, 3}, result.getSequences()[1]);
        assertArrayEquals(new int[] {0, 20}, result.getStartTimes()[0]);
        assertArrayEquals(new int[] {0, 10}, result.getStartTimes()[1]);
    }

    @Test
    void applyDoesNotMutateOriginal() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var origSeq0 = solution.getSequences()[0].clone();
        var origSeq1 = solution.getSequences()[1].clone();
        final var origTotalRevenue = solution.getTotalRevenue();
        new InterSwapMove(problem, solution, 0, 0, 1, 0).apply();
        assertArrayEquals(origSeq0, solution.getSequences()[0]);
        assertArrayEquals(origSeq1, solution.getSequences()[1]);
        assertEquals(origTotalRevenue, solution.getTotalRevenue(), 1e-6);
    }

    @Test
    void applyUpdatesTotalInvDuration() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var result = new InterSwapMove(problem, solution, 0, 0, 1, 0).apply();
        assertEquals(20, result.getTotalInvDuration()[0]);
        assertEquals(10, result.getTotalInvDuration()[1]);
    }

    @Test
    void applyUpdatesTotalDurationOfHourDifferentHours() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 1, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var result = new InterSwapMove(problem, solution, 0, 0, 1, 0).apply();
        assertEquals(20, result.getTotalDurationOfHour()[0]);
        assertEquals(10, result.getTotalDurationOfHour()[1]);
    }

    @Test
    void applyPassesFeasibilityCheck() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void applyTotalRevenueMatchesGain() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new InterSwapMove(problem, solution, 0, 0, 1, 0);
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

            for (int attempt = 0; attempt < 10; attempt++) {
                var candidateInvs = new ArrayList<Integer>();
                for (int inv = 0; inv < problem.getInventories().length; inv++) {
                    if (solution.getSequences()[inv].length >= 1) {
                        candidateInvs.add(inv);
                    }
                }
                if (candidateInvs.size() < 2) {
                    break;
                }
                var idx1 = rng.nextInt(candidateInvs.size());
                var idx2 = rng.nextInt(candidateInvs.size());
                if (idx1 == idx2) {
                    continue;
                }
                var invId1 = candidateInvs.get(idx1);
                var invId2 = candidateInvs.get(idx2);
                var p1 = rng.nextInt(solution.getSequences()[invId1].length);
                var p2 = rng.nextInt(solution.getSequences()[invId2].length);

                var comm1Id = solution.getSequences()[invId1][p1];
                var comm2Id = solution.getSequences()[invId2][p2];
                var dur1 = problem.getCommercial(comm1Id).getDuration();
                var dur2 = problem.getCommercial(comm2Id).getDuration();
                var delta = dur2 - dur1;
                var newDur1 = solution.getTotalInvDuration()[invId1] + delta;
                var newDur2 = solution.getTotalInvDuration()[invId2] - delta;
                if (newDur1 > problem.getInventory(invId1).getDuration()) {
                    continue;
                }
                if (newDur2 > problem.getInventory(invId2).getDuration()) {
                    continue;
                }

                var move = new InterSwapMove(problem, solution, invId1, p1, invId2, p2);
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
