package scheduling.solver.heuristic.grasp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class GraspSolutionTest {

    private static Problem problem;

    @BeforeAll
    static void setUp() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories =
                new Inventory[] {new Inventory(0, 120, 0, 5), new Inventory(1, 120, 0, 5)};
        problem = buildProblem(commercials, inventories);
    }

    @Test
    void assignedArraysReflectSequences() {
        var solution = buildSolution(problem, new int[][] {{0, 1}, {2}});
        assertArrayEquals(new int[] {0, 0, 1}, solution.getAssignedInvId());
        assertArrayEquals(new int[] {0, 1, 0}, solution.getAssignedPos());
    }

    @Test
    void unassignedCommercialsHaveMinusOne() {
        var solution = buildSolution(problem, new int[][] {{0}, {2}});
        assertArrayEquals(new int[] {0, -1, 1}, solution.getAssignedInvId());
        assertArrayEquals(new int[] {0, -1, 0}, solution.getAssignedPos());
    }

    @Test
    void builderMaintainsAssignedArraysAfterReplaceSequence() {
        var solution = buildSolution(problem, new int[][] {{0, 1}, {2}});
        var result =
                solution.toBuilder(problem)
                        .replaceSequence(0, new int[] {1}, 0)
                        .replaceSequence(1, new int[] {0, 2}, 0)
                        .build();
        assertArrayEquals(new int[] {1, 0, 1}, result.getAssignedInvId());
        assertArrayEquals(new int[] {0, 0, 1}, result.getAssignedPos());
    }

    @Test
    void emptySequencesAllUnassigned() {
        var solution = buildSolution(problem, new int[][] {{}, {}});
        assertArrayEquals(new int[] {-1, -1, -1}, solution.getAssignedInvId());
        assertArrayEquals(new int[] {-1, -1, -1}, solution.getAssignedPos());
    }

    private static Problem buildProblem(Commercial[] commercials, Inventory[] inventories) {
        var numComm = commercials.length;
        var numInv = inventories.length;
        var suitability = new boolean[numComm][numInv];
        for (int c = 0; c < numComm; c++) {
            Arrays.fill(suitability[c], true);
        }
        var suitInvFor = new int[numComm][];
        for (int c = 0; c < numComm; c++) {
            var list = new ArrayList<Integer>();
            for (int i = 0; i < numInv; i++) {
                if (suitability[c][i]) {
                    list.add(i);
                }
            }
            suitInvFor[c] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        var suitCommFor = new int[numInv][];
        for (int i = 0; i < numInv; i++) {
            var list = new ArrayList<Integer>();
            for (int c = 0; c < numComm; c++) {
                if (suitability[c][i]) {
                    list.add(c);
                }
            }
            suitCommFor[i] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        var attentionTypes = new AttentionType[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                attentionTypes[c][i] = new AttentionType[] {AttentionType.N};
            }
        }
        var revenueMatrix = new double[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                revenueMatrix[c][i] = new double[inventories[i].getDuration() + 1];
                Arrays.fill(revenueMatrix[c][i], 100.0 * (c + 1));
            }
        }
        var hours = Arrays.stream(inventories).mapToInt(Inventory::getHour).distinct().toArray();
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

    private static GraspSolution buildSolution(Problem problem, int[][] sequences) {
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
}
