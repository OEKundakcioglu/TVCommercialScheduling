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

class TransferMoveTest {

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
    void feasibleBasicTransfer() {
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
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleTransferToEmptyInventory() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleNotSuitable() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var suitability = new boolean[][] {{true, false}};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        suitability,
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleCommercialCountExceeded() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 1);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleDurationExceeded() {
        var comm0 = new Commercial(0, 1, 0, 30, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 20, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleHourlyLimit() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 720, 0, 5);
        var inv1 = new Inventory(1, 720, 1, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var sequences = new int[][] {{0}, {}};
        var sol = buildSolution(problem, sequences);
        var totalDurationOfHour = new int[] {10, 715};
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
        var move = new TransferMove(problem, tweakedSolution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleHourlyLimitSameHour() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 720, 0, 5);
        var inv1 = new Inventory(1, 720, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAtSourceGap() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 2, 0, 10, 300.0, PricingType.FIXED);
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
        // inv0: [comm2, comm0, comm1] — removing comm0 makes comm2 and comm1 adjacent (same group)
        var solution = buildCheckedSolution(problem, new int[][] {{2, 0, 1}, {}});
        var move = new TransferMove(problem, solution, 0, 1, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAtDestination() {
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
        // Transferring comm0 next to comm1 (same group)
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionAtDestination() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var attentionTypes = buildNAttentionTypes(2, 2);
        attentionTypes[0][1] = new AttentionType[] {AttentionType.L1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        attentionTypes,
                        buildConstantRevenueMatrix(2, 2, inventories));
        // Transferring comm0 to position 0 of inv1 which has comm1.
        // comm0 needs L1 at inv1, but it would be at position 0 of a 2-element sequence.
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionShiftedCommAtDestination() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var attentionTypes = buildNAttentionTypes(2, 2);
        attentionTypes[1][1] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        attentionTypes,
                        buildConstantRevenueMatrix(2, 2, inventories));
        // inv1 has [comm1] with F1. Inserting comm0 at position 0 pushes comm1 to position 1.
        // comm1 has F1 at inv1, position 1 does not satisfy F1.
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleTransferFromPos2WithShiftedLTypeCommercial() {
        // Boundary condition: removing from fromPos=2 shifts position 3 into the F-zone (position
        // 2).
        // The checkSourceAttentionFeasibility guard uses fromPos <= 1, so this shifted commercial
        // is NOT explicitly re-checked for F-type attention. This is safe because:
        //   - No F-type flag is satisfied at position >= 3 (max is F3 = position 2, 0-indexed)
        //   - L-type flags are invariant under simultaneous position(-1) and length(-1) shifts
        // This test documents that boundary and would catch a regression if attention types
        // expanded.
        //
        // Source inv0: [comm0(g1), comm1(g2), comm2(g3), comm3(g4)] — length 4
        //   comm3 at position 3 has L1 attention (requires last position = sequenceLength-1 = 3)
        // Transfer comm2 from inv0:2 to inv1:0
        // After: inv0 becomes [comm0, comm1, comm3] — comm3 shifts to position 2, length 3
        //   comm3 L1 at (2, 3): sequenceLength-1 = 2 == position 2 → still satisfied
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 10, 400.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3};
        var inv0 = new Inventory(0, 120, 0, 10);
        var inv1 = new Inventory(1, 120, 1, 10);
        var inventories = new Inventory[] {inv0, inv1};
        var attentionTypes = buildNAttentionTypes(4, 2);
        attentionTypes[3][0] = new AttentionType[] {AttentionType.L1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 2),
                        attentionTypes,
                        buildConstantRevenueMatrix(4, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2, 3}, {}});

        var move = new TransferMove(problem, solution, 0, 2, 1, 0);
        assertTrue(move.checkFeasibility());
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
        assertArrayEquals(new int[] {0, 1, 3}, result.getSequences()[0]);
        assertArrayEquals(new int[] {2}, result.getSequences()[1]);
    }

    @Test
    void feasibleTransferFromPos2WithL123ShiftedCommercial() {
        // Similar boundary: comm3 has L123 at source. At old (3, 4): 3 >= 4-3=1 ✅.
        // After shift to (2, 3): 2 >= 3-3=0 ✅. Still satisfied without explicit F-zone re-check.
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 10, 400.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3};
        var inv0 = new Inventory(0, 120, 0, 10);
        var inv1 = new Inventory(1, 120, 1, 10);
        var inventories = new Inventory[] {inv0, inv1};
        var attentionTypes = buildNAttentionTypes(4, 2);
        attentionTypes[3][0] = new AttentionType[] {AttentionType.L123};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 2),
                        attentionTypes,
                        buildConstantRevenueMatrix(4, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2, 3}, {}});

        var move = new TransferMove(problem, solution, 0, 2, 1, 0);
        assertTrue(move.checkFeasibility());
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    // --- calculateRevenueGain ---

    @Test
    void revenueGainWithConstantRevenue() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainWithTimeVaryingRevenue() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var revenueMatrix = new double[3][2][];
        for (int c = 0; c < 3; c++) {
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
                        buildAllSuitable(3, 2),
                        buildNAttentionTypes(3, 2),
                        revenueMatrix);
        // inv0: [comm0(dur=10), comm1(dur=10)] → startTimes [0, 10]
        // inv1: [comm2(dur=10)] → startTimes [0]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}, {2}});
        // Transfer comm0 from inv0:0 to inv1:0
        // comm0: was rev(0,0,0)=100, now rev(0,1,0)=100 → delta=0
        // comm1 in inv0: was at t=10 rev=210, now at t=0 rev=200 → delta=-10
        // comm2 in inv1: was at t=0 rev=300, now at t=10 rev=310 → delta=+10
        // total gain = 0
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    // --- apply ---

    @Test
    void applyTransfersCommercial() {
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
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}, {}});
        var result = new TransferMove(problem, solution, 0, 0, 1, 0).apply();
        assertArrayEquals(new int[] {1}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0}, result.getSequences()[1]);
    }

    @Test
    void applyRebuildsStartTimes() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED);
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
        // inv0: [comm0(10), comm1(20)] → startTimes [0, 10]
        // inv1: [comm2(15)] → startTimes [0]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}, {2}});
        // Transfer comm0 from inv0:0 to inv1:1
        var result = new TransferMove(problem, solution, 0, 0, 1, 1).apply();
        assertArrayEquals(new int[] {1}, result.getSequences()[0]);
        assertArrayEquals(new int[] {2, 0}, result.getSequences()[1]);
        assertArrayEquals(new int[] {0}, result.getStartTimes()[0]);
        assertArrayEquals(new int[] {0, 15}, result.getStartTimes()[1]);
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
        new TransferMove(problem, solution, 0, 0, 1, 0).apply();
        assertArrayEquals(origSeq0, solution.getSequences()[0]);
        assertArrayEquals(origSeq1, solution.getSequences()[1]);
        assertEquals(origTotalRevenue, solution.getTotalRevenue(), 1e-6);
    }

    @Test
    void applyUpdatesTotalInvDuration() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var result = new TransferMove(problem, solution, 0, 0, 1, 0).apply();
        assertEquals(0, result.getTotalInvDuration()[0]);
        assertEquals(10, result.getTotalInvDuration()[1]);
    }

    @Test
    void applyUpdatesTotalDurationOfHourDifferentHours() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 720, 0, 5);
        var inv1 = new Inventory(1, 720, 1, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 2),
                        buildNAttentionTypes(1, 2),
                        buildConstantRevenueMatrix(1, 2, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var result = new TransferMove(problem, solution, 0, 0, 1, 0).apply();
        assertEquals(0, result.getTotalDurationOfHour()[0]);
        assertEquals(10, result.getTotalDurationOfHour()[1]);
    }

    @Test
    void applyPassesFeasibilityCheck() {
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
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void applyTotalRevenueMatchesGain() {
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
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}, {}});
        var move = new TransferMove(problem, solution, 0, 0, 1, 0);
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
                var candidateFromInvs = new ArrayList<Integer>();
                for (int inv = 0; inv < problem.getInventories().length; inv++) {
                    if (solution.getSequences()[inv].length >= 1) {
                        candidateFromInvs.add(inv);
                    }
                }
                if (candidateFromInvs.isEmpty()) {
                    break;
                }
                var fromInvId = candidateFromInvs.get(rng.nextInt(candidateFromInvs.size()));
                var toInvId = rng.nextInt(problem.getInventories().length);
                if (fromInvId == toInvId) {
                    continue;
                }
                var fromPos = rng.nextInt(solution.getSequences()[fromInvId].length);
                var toPos = rng.nextInt(solution.getSequences()[toInvId].length + 1);

                var commId = solution.getSequences()[fromInvId][fromPos];
                var commDur = problem.getCommercial(commId).getDuration();
                var newDestDur = solution.getTotalInvDuration()[toInvId] + commDur;
                if (newDestDur > problem.getInventory(toInvId).getDuration()) {
                    continue;
                }

                var move = new TransferMove(problem, solution, fromInvId, fromPos, toInvId, toPos);
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
