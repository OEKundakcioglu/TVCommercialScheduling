package scheduling.solver.heuristic.grasp.pathrelinking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.heuristic.grasp.GraspSolution;

class PathRelinkingUtilsTest {

    private Problem problem;
    private int numComm;
    private Commercial[] commercials;
    private Inventory[] inventories;
    private boolean[][] suitability;
    private AttentionType[][][] attentionTypes;
    private double[][][] revenueMatrix;

    @BeforeEach
    void setUp() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        inventories = new Inventory[] {inv0, inv1};
        suitability = buildAllSuitable(3, 2);
        attentionTypes = buildNAttentionTypes(3, 2);
        revenueMatrix = buildConstantRevenueMatrix(3, 2, inventories);
        problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        numComm = problem.getCommercials().length;
    }

    @Test
    void distanceIdenticalSolutionsIsZero() {
        var s1 = buildSolution(problem, new int[][] {{0, 1}, {2}});
        var s2 = buildSolution(problem, new int[][] {{0, 1}, {2}});
        assertEquals(0, PathRelinkingUtils.distance(s1, s2, 3));
    }

    @Test
    void distanceCountsTransfers() {
        var s1 = buildSolution(problem, new int[][] {{0}, {1}});
        var s2 = buildSolution(problem, new int[][] {{1}, {0}});
        assertEquals(2, PathRelinkingUtils.distance(s1, s2, 2));
    }

    @Test
    void distanceCountsInsertsAndRemoves() {
        var s1 = buildSolution(problem, new int[][] {{0}, {}});
        var s2 = buildSolution(problem, new int[][] {{}, {1}});
        assertEquals(2, PathRelinkingUtils.distance(s1, s2, 3));
    }

    @Test
    void distanceBothUnassignedNotCounted() {
        var s1 = buildSolution(problem, new int[][] {{0}, {}});
        var s2 = buildSolution(problem, new int[][] {{0}, {}});
        assertEquals(0, PathRelinkingUtils.distance(s1, s2, 3));
    }

    @Test
    void distanceSameInventoryDifferentPositionIsZero() {
        var s1 = buildSolution(problem, new int[][] {{0, 1}, {}});
        var s2 = buildSolution(problem, new int[][] {{1, 0}, {}});
        assertEquals(0, PathRelinkingUtils.distance(s1, s2, 3));
    }

    @Test
    void selectMoveReturnsNullForIdenticalSolutions() {
        var s1 = buildSolution(problem, new int[][] {{0, 1}, {2}});
        var s2 = buildSolution(problem, new int[][] {{0, 1}, {2}});
        var move = PathRelinkingUtils.selectMove(problem, s1, s2, new Random(42));
        assertTrue(move.isEmpty());
    }

    @Test
    void selectMoveReturnsTransferWhenDifferentInventories() {
        var current = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var guiding = buildCheckedSolution(problem, new int[][] {{}, {0}});
        var moveOpt = PathRelinkingUtils.selectMove(problem, current, guiding, new Random(42));
        assertTrue(moveOpt.isPresent());
        var result = moveOpt.get().apply();
        var newDist = PathRelinkingUtils.distance(result, guiding, numComm);
        assertTrue(newDist < PathRelinkingUtils.distance(current, guiding, numComm));
    }

    @Test
    void selectMoveReturnsRemoveWhenOnlyInCurrent() {
        var current = buildCheckedSolution(problem, new int[][] {{0, 1}, {}});
        var guiding = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var moveOpt = PathRelinkingUtils.selectMove(problem, current, guiding, new Random(42));
        assertTrue(moveOpt.isPresent());
        var result = moveOpt.get().apply();
        var newDist = PathRelinkingUtils.distance(result, guiding, numComm);
        assertTrue(newDist < PathRelinkingUtils.distance(current, guiding, numComm));
    }

    @Test
    void selectMoveReturnsInsertWhenOnlyInGuiding() {
        var current = buildCheckedSolution(problem, new int[][] {{0}, {}});
        var guiding = buildCheckedSolution(problem, new int[][] {{0, 1}, {}});
        var moveOpt = PathRelinkingUtils.selectMove(problem, current, guiding, new Random(42));
        assertTrue(moveOpt.isPresent());
        var result = moveOpt.get().apply();
        var newDist = PathRelinkingUtils.distance(result, guiding, numComm);
        assertTrue(newDist < PathRelinkingUtils.distance(current, guiding, numComm));
    }

    @Test
    void selectMoveReturnsNullWhenAllMovesInfeasible() {
        var restrictedSuitability = new boolean[][] {{true, false}, {true, true}, {true, true}};
        var restrictedProblem =
                buildProblem(
                        commercials,
                        inventories,
                        restrictedSuitability,
                        attentionTypes,
                        revenueMatrix);
        var current = buildCheckedSolution(restrictedProblem, new int[][] {{0}, {}});
        var guiding = buildSolution(restrictedProblem, new int[][] {{}, {0}});
        var move =
                PathRelinkingUtils.selectMove(restrictedProblem, current, guiding, new Random(42));
        assertTrue(move.isEmpty());
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
}
