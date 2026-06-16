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

class InsertMoveTest {

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

    @Test
    void feasibleInsertIntoEmptyInventory() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 1),
                        buildNAttentionTypes(1, 1),
                        buildConstantRevenueMatrix(1, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{}});
        var move = new InsertMove(problem, solution, 0, 0, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleInsertAtBeginning() {
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 0, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleInsertAtEnd() {
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleInsertInMiddle() {
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
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        var move = new InsertMove(problem, solution, 0, 1, 2);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionInsertedCommercial() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var attentionTypes = new AttentionType[2][1][];
        attentionTypes[0][0] = new AttentionType[] {AttentionType.N};
        attentionTypes[1][0] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleFTypeShiftedCommercial() {
        var attentionTypes = new AttentionType[3][1][];
        attentionTypes[0][0] = new AttentionType[] {AttentionType.F1};
        attentionTypes[1][0] = new AttentionType[] {AttentionType.N};
        attentionTypes[2][0] = new AttentionType[] {AttentionType.N};
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        var move = new InsertMove(problem, solution, 0, 0, 2);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleLTypeNonShiftedCommercial() {
        var attentionTypes = new AttentionType[3][1][];
        attentionTypes[0][0] = new AttentionType[] {AttentionType.N};
        attentionTypes[1][0] = new AttentionType[] {AttentionType.L1};
        attentionTypes[2][0] = new AttentionType[] {AttentionType.N};
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        var move = new InsertMove(problem, solution, 0, 2, 2);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupLeftNeighbor() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 10, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupRightNeighbor() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 10, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleCommercialCountExceeded() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 1);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void feasibleCommercialCountNotExceeded() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 120, 1, 2);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleDuration() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 15, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleHourlyLimit() {
        var comm0 = new Commercial(0, 1, 0, 700, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 30, 200.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 750, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void revenueGainInsertIntoEmpty() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var revenueMatrix = new double[1][1][];
        revenueMatrix[0][0] = new double[121];
        Arrays.fill(revenueMatrix[0][0], 1000.0);
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 1),
                        buildNAttentionTypes(1, 1),
                        revenueMatrix);
        var solution = buildSolution(problem, new int[][] {{}});
        var move = new InsertMove(problem, solution, 0, 0, 0);
        assertEquals(1000.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainInsertAtEnd() {
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        Arrays.fill(revenueMatrix[0][0], 100.0);
        revenueMatrix[1][0] = new double[121];
        Arrays.fill(revenueMatrix[1][0], 200.0);
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertEquals(200.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainInsertAtBeginningWithShift() {
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        revenueMatrix[1][0] = new double[121];
        for (int t = 0; t < 121; t++) {
            revenueMatrix[0][0][t] = 100.0 + t * 2.0;
            revenueMatrix[1][0][t] = 200.0 + t * 2.0;
        }
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 5, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 0, 1);
        assertEquals(210.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void applyInsertIntoEmpty() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var revenueMatrix = new double[1][1][];
        revenueMatrix[0][0] = new double[121];
        Arrays.fill(revenueMatrix[0][0], 1000.0);
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(1, 1),
                        buildNAttentionTypes(1, 1),
                        revenueMatrix);
        var solution = buildSolution(problem, new int[][] {{}});
        var move = new InsertMove(problem, solution, 0, 0, 0);
        var result = move.apply();
        assertArrayEquals(new int[] {0}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0}, result.getStartTimes()[0]);
        assertEquals(1000.0, result.getRevenues()[0][0], 1e-6);
        assertEquals(1000.0, result.getTotalRevenue(), 1e-6);
        assertEquals(10, result.getTotalDurationOfHour()[1]);
    }

    @Test
    void applyInsertAtBeginning() {
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        Arrays.fill(revenueMatrix[0][0], 100.0);
        revenueMatrix[1][0] = new double[121];
        Arrays.fill(revenueMatrix[1][0], 200.0);
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 5, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 0, 1);
        var result = move.apply();
        assertArrayEquals(new int[] {1, 0}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0, 5}, result.getStartTimes()[0]);
        assertEquals(200.0, result.getRevenues()[0][0], 1e-6);
        assertEquals(100.0, result.getRevenues()[0][1], 1e-6);
        assertEquals(
                solution.getTotalRevenue() + move.calculateRevenueGain(),
                result.getTotalRevenue(),
                1e-6);
    }

    @Test
    void applyInsertAtEnd() {
        var revenueMatrix = new double[2][1][];
        revenueMatrix[0][0] = new double[121];
        Arrays.fill(revenueMatrix[0][0], 100.0);
        revenueMatrix[1][0] = new double[121];
        Arrays.fill(revenueMatrix[1][0], 200.0);
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        var result = move.apply();
        assertArrayEquals(new int[] {0, 1}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0, 10}, result.getStartTimes()[0]);
        assertEquals(100.0, result.getRevenues()[0][0], 1e-6);
        assertEquals(200.0, result.getRevenues()[0][1], 1e-6);
        assertEquals(
                solution.getTotalRevenue() + move.calculateRevenueGain(),
                result.getTotalRevenue(),
                1e-6);
    }

    @Test
    void applyDoesNotMutateOriginal() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 5, 200.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var originalSeq = solution.getSequences()[0].clone();
        var originalStartTimes = solution.getStartTimes()[0].clone();

        var move = new InsertMove(problem, solution, 0, 0, 1);
        final var originalRevenues = solution.getRevenues()[0].clone();
        final var originalTotalRevenue = solution.getTotalRevenue();
        final var originalTotalDurationOfHour = solution.getTotalDurationOfHour().clone();
        move.apply();

        assertArrayEquals(originalSeq, solution.getSequences()[0]);
        assertArrayEquals(originalStartTimes, solution.getStartTimes()[0]);
        assertEquals(originalRevenues.length, solution.getRevenues()[0].length);
        for (int i = 0; i < originalRevenues.length; i++) {
            assertEquals(originalRevenues[i], solution.getRevenues()[0][i], 1e-6);
        }
        assertEquals(originalTotalRevenue, solution.getTotalRevenue(), 1e-6);
        assertArrayEquals(originalTotalDurationOfHour, solution.getTotalDurationOfHour());
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
        var solution = buildSolution(problem, new int[][] {{0}});
        var move = new InsertMove(problem, solution, 0, 1, 1);
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    // --- checkAttentionFeasibility ---

    @Test
    void attentionF123ShiftedFromPos2ToPos3IsInfeasible() {
        // comm0(F123) at pos 2, insert comm3 at pos 0 → comm0 shifts to pos 3, F123 requires <=2
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 100.0, PricingType.FIXED);
        var comm3 = new Commercial(3, 4, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2, comm3};
        var inv0 = new Inventory(0, 120, 1, 5);
        var inventories = new Inventory[] {inv0};
        var attentionTypes = buildNAttentionTypes(4, 1);
        attentionTypes[0][0] = new AttentionType[] {AttentionType.F123};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(4, 1, inventories));
        // sequence: [comm1, comm2, comm0] — comm0 at pos 2, F123 satisfied (2 <= 2)
        var solution = buildSolution(problem, new int[][] {{1, 2, 0}});
        // insert comm3 at pos 0 → new sequence [comm3, comm1, comm2, comm0], comm0 at pos 3
        var move = new InsertMove(problem, solution, 0, 0, 3);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void attentionL1InsertedAtEndIsFeasible() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0}});
        // insert comm1(L1) at pos 1 (end), new length=2, L1 requires pos==1 ✓
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void attentionSkippedWhenInsertingInMiddleOfLongSequence() {
        var commercials = new Commercial[10];
        for (int c = 0; c < 10; c++) {
            commercials[c] = new Commercial(c, c + 1, 0, 5, 100.0, PricingType.FIXED);
        }
        var inv0 = new Inventory(0, 200, 1, 20);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(10, 1),
                        buildNAttentionTypes(10, 1),
                        buildConstantRevenueMatrix(10, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0, 1, 2, 3, 4, 5, 6, 7}});
        // insert comm8 at pos 4 — middle of a length-8 sequence
        var move = new InsertMove(problem, solution, 0, 4, 8);
        assertTrue(move.checkFeasibility());
    }

    // --- checkGroupFeasibility ---

    @Test
    void groupFeasibleDifferentGroupsBothSides() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 100.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        // insert comm2(group=3) between comm0(group=1) and comm1(group=2)
        var move = new InsertMove(problem, solution, 0, 1, 2);
        assertTrue(move.checkFeasibility());
    }

    // --- checkDurationFeasibility ---

    @Test
    void durationExactFitIsFeasible() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 20, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        // 10 + 10 = 20 = inventory duration exactly
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertTrue(move.checkFeasibility());
    }

    // --- checkHourlyLimitFeasibility ---

    @Test
    void hourlyLimitExact720IsFeasible() {
        var comm0 = new Commercial(0, 1, 0, 360, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 360, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 720, 1, 5);
        var inventories = new Inventory[] {inv0};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildSolution(problem, new int[][] {{0}});
        // 360 + 360 = 720 = exactly the limit
        var move = new InsertMove(problem, solution, 0, 1, 1);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void hourlyLimitAccountsForOtherInventoriesInSameHour() {
        var comm0 = new Commercial(0, 1, 0, 400, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 400, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1};
        var inv0 = new Inventory(0, 500, 1, 5);
        var inv1 = new Inventory(1, 500, 1, 5); // same hour
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 2),
                        buildNAttentionTypes(2, 2),
                        buildConstantRevenueMatrix(2, 2, inventories));
        // comm0 in inv0 (400s in hour 1), insert comm1 into inv1 → total 800 > 720
        var solution = buildSolution(problem, new int[][] {{0}, {}});
        var move = new InsertMove(problem, solution, 1, 0, 1);
        assertFalse(move.checkFeasibility());
    }

    // --- buildNewSequence (tested via apply) ---

    @Test
    void applyBuildsCorrectSequenceForMiddleInsert() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 100.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        var result = new InsertMove(problem, solution, 0, 1, 2).apply();
        assertArrayEquals(new int[] {0, 2, 1}, result.getSequences()[0]);
    }

    // --- rebuildStartTimesAndRevenues (tested via apply) ---

    @Test
    void applyRebuildsStartTimesWithVaryingDurations() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 25, 100.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 7, 100.0, PricingType.FIXED);
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
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        // insert comm2(dur=7) at pos 1 → [comm0(10), comm2(7), comm1(25)]
        var result = new InsertMove(problem, solution, 0, 1, 2).apply();
        assertArrayEquals(new int[] {0, 10, 17}, result.getStartTimes()[0]);
    }

    // --- buildNewTotalDurationOfHour (tested via apply) ---

    @Test
    void applyUpdatesOnlyAffectedHour() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 100.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 15, 100.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 1, 5); // hour 1
        var inv1 = new Inventory(1, 120, 2, 5); // hour 2
        var inventories = new Inventory[] {inv0, inv1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 2),
                        buildNAttentionTypes(3, 2),
                        buildConstantRevenueMatrix(3, 2, inventories));
        // comm0 in inv0 (hour 1, 10s), comm1 in inv1 (hour 2, 20s)
        var solution = buildSolution(problem, new int[][] {{0}, {1}});
        var originalHour1 = solution.getTotalDurationOfHour()[1];
        var originalHour2 = solution.getTotalDurationOfHour()[2];
        // insert comm2(dur=15) into inv0 (hour 1)
        var result = new InsertMove(problem, solution, 0, 1, 2).apply();
        assertEquals(originalHour1 + 15, result.getTotalDurationOfHour()[1]);
        assertEquals(originalHour2, result.getTotalDurationOfHour()[2]);
    }

    // --- calculateRevenueChange (tested via calculateRevenueGain) ---

    @Test
    void revenueGainWithMultipleShiftedCommercials() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 5, 100.0, PricingType.FIXED);
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
        // [comm0(dur=10), comm1(dur=10)] → startTimes [0, 10]
        var solution = buildSolution(problem, new int[][] {{0, 1}});
        // insert comm2(dur=5) at pos 0 → both comm0 and comm1 shift by 5
        var move = new InsertMove(problem, solution, 0, 0, 2);
        // comm2 at t=0: 300.0
        // comm0 shift: was t=0 (100.0), now t=5 (105.0), delta = +5.0
        // comm1 shift: was t=10 (210.0), now t=15 (215.0), delta = +5.0
        // total gain = 300.0 + 5.0 + 5.0 = 310.0
        assertEquals(310.0, move.calculateRevenueGain(), 1e-6);
    }

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

            var assigned = new HashSet<Integer>();
            for (var seq : solution.getSequences()) {
                for (var commId : seq) {
                    assigned.add(commId);
                }
            }
            var unassigned = new ArrayList<Integer>();
            for (int c = 0; c < problem.getCommercials().length; c++) {
                if (!assigned.contains(c)) {
                    unassigned.add(c);
                }
            }
            if (unassigned.isEmpty()) {
                continue;
            }

            for (int attempt = 0; attempt < 10; attempt++) {
                var commId = unassigned.get(rng.nextInt(unassigned.size()));
                var suitableInvs = problem.getSuitableInventories(commId);
                if (suitableInvs.length == 0) {
                    continue;
                }
                var invId = suitableInvs[rng.nextInt(suitableInvs.length)];
                var position = rng.nextInt(solution.getSequences()[invId].length + 1);

                var seq = solution.getSequences()[invId];
                var currentDuration = 0;
                for (var commIdInSeq : seq) {
                    currentDuration += problem.getCommercial(commIdInSeq).getDuration();
                }
                var insertDuration = problem.getCommercial(commId).getDuration();
                if (currentDuration + insertDuration > problem.getInventory(invId).getDuration()) {
                    continue;
                }

                var move = new InsertMove(problem, solution, invId, position, commId);
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
