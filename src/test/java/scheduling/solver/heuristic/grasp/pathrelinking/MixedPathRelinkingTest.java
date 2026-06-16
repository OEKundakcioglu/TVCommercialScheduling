package scheduling.solver.heuristic.grasp.pathrelinking;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

class MixedPathRelinkingTest {

    private Problem problem;
    private int numComm;

    @BeforeEach
    void setUp() {
        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED);
        var comm2 = new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED);
        var commercials = new Commercial[] {comm0, comm1, comm2};
        var inv0 = new Inventory(0, 120, 0, 5);
        var inv1 = new Inventory(1, 120, 0, 5);
        var inventories = new Inventory[] {inv0, inv1};
        var suitability = buildAllSuitable(3, 2);
        var attentionTypes = buildNAttentionTypes(3, 2);
        var revenueMatrix = buildConstantRevenueMatrix(3, 2, inventories);
        problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        numComm = problem.getCommercials().length;
    }

    @Test
    void relinkIdenticalSolutionsReturnsInitial() {
        var s = buildCheckedSolution(problem, new int[][] {{0, 1}, {2}});
        var pr = new MixedPathRelinking(problem, s, s, new Random(42));
        var result = pr.relink();
        assertEquals(s.getTotalRevenue(), result.getTotalRevenue(), 1e-6);
    }

    @Test
    void relinkReturnsFeasibleSolution() {
        var initial = buildCheckedSolution(problem, new int[][] {{0, 1}, {2}});
        var guiding = buildCheckedSolution(problem, new int[][] {{2}, {0, 1}});
        var pr = new MixedPathRelinking(problem, initial, guiding, new Random(42));
        var result = pr.relink();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void relinkReturnsAtLeastAsGoodAsInitial() {
        var initial = buildCheckedSolution(problem, new int[][] {{0, 1}, {2}});
        var guiding = buildCheckedSolution(problem, new int[][] {{2}, {0, 1}});
        var pr = new MixedPathRelinking(problem, initial, guiding, new Random(42));
        var result = pr.relink();
        assertTrue(result.getTotalRevenue() >= initial.getTotalRevenue());
    }

    @Test
    void relinkReducesDistanceToGuiding() {
        var initial = buildCheckedSolution(problem, new int[][] {{0}, {1}});
        var guiding = buildCheckedSolution(problem, new int[][] {{1}, {0}});
        var pr = new MixedPathRelinking(problem, initial, guiding, new Random(42));
        var result = pr.relink();
        var initialDist = PathRelinkingUtils.distance(initial, guiding, numComm);
        var resultDist = PathRelinkingUtils.distance(result, guiding, numComm);
        assertTrue(resultDist <= initialDist);
    }

    @Test
    @SuppressFBWarnings(
            value = "DMI_RANDOM_USED_ONLY_ONCE",
            justification = "Isolated RNG per trial for deterministic reproducibility")
    void bruteForceRelinkAlwaysProducesFeasibleSolutions() {
        var rng = new Random(42);
        for (int trial = 0; trial < 200; trial++) {
            var problem = buildRandomProblem(rng);
            var initial = buildRandomFeasibleSolution(problem, rng);
            var guiding = buildRandomFeasibleSolution(problem, rng);
            var trialRng = new Random(rng.nextLong());
            var pr = new MixedPathRelinking(problem, initial, guiding, trialRng);
            var result = pr.relink();
            assertDoesNotThrow(
                    () -> FeasibilityCheck.check(problem, result),
                    "Infeasible result at trial=" + trial);
            assertTrue(
                    result.getTotalRevenue() >= initial.getTotalRevenue() - 1e-6,
                    "Result worse than initial at trial=" + trial);
        }
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
}
