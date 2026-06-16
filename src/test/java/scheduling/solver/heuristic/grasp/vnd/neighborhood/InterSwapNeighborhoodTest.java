package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.heuristic.grasp.GraspSolution;

class InterSwapNeighborhoodTest {

    @Test
    void generatesNoMovesWhenFewerThanTwoNonEmptyInventories() {
        var comms =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                };
        var invs =
                new Inventory[] {
                    new Inventory(0, 60, 0, 10), new Inventory(1, 60, 0, 10),
                };
        var suit = new boolean[][] {{true, true}};
        var attn = new AttentionType[][][] {{{AttentionType.N}, {AttentionType.N}}};
        var rev = buildConstantRevenueMatrix(1, 2, invs, 10.0);
        var problem = buildProblem(comms, invs, suit, attn, rev);
        var solution = buildSolution(problem, new int[][] {{0}, {}});

        var random = new Random(42);
        var neighborhood = new InterSwapNeighborhood(problem);
        var moves = neighborhood.generateMoves(solution, random);

        assertFalse(moves.iterator().hasNext());
    }

    @Test
    void generatesCorrectMoveCountBasicCase() {
        var comms =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED),
                };
        var invs =
                new Inventory[] {
                    new Inventory(0, 60, 0, 10), new Inventory(1, 60, 0, 10),
                };
        var suit = new boolean[][] {{true, true}, {true, true}};
        var attn =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                };
        var rev = buildConstantRevenueMatrix(2, 2, invs, 10.0);
        var problem = buildProblem(comms, invs, suit, attn, rev);
        var solution = buildSolution(problem, new int[][] {{0}, {1}});

        var random = new Random(42);
        var neighborhood = new InterSwapNeighborhood(problem);
        var moveCount = 0;
        for (var unused : neighborhood.generateMoves(solution, random)) {
            moveCount++;
        }

        // inv1->inv2: 1 pos x 1 pos = 1, inv2->inv1: 1 pos x 1 pos = 1 -> total = 2
        assertEquals(2, moveCount);
    }

    @Test
    void generatesCorrectMoveCountWithMultiplePositions() {
        var comms =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 100.0, PricingType.FIXED),
                };
        var invs =
                new Inventory[] {
                    new Inventory(0, 60, 0, 10), new Inventory(1, 60, 0, 10),
                };
        var suit = new boolean[][] {{true, true}, {true, true}, {true, true}};
        var attn =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                };
        var rev = buildConstantRevenueMatrix(3, 2, invs, 10.0);
        var problem = buildProblem(comms, invs, suit, attn, rev);
        var solution = buildSolution(problem, new int[][] {{0, 1}, {2}});

        var random = new Random(42);
        var neighborhood = new InterSwapNeighborhood(problem);
        var moveCount = 0;
        for (var unused : neighborhood.generateMoves(solution, random)) {
            moveCount++;
        }

        assertEquals(4, moveCount);
    }

    @Test
    void skipsEmptyInventories() {
        var comms =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED),
                };
        var invs =
                new Inventory[] {
                    new Inventory(0, 60, 0, 10),
                    new Inventory(1, 60, 0, 10),
                    new Inventory(2, 60, 0, 10),
                };
        var suit = new boolean[][] {{true, true, true}, {true, true, true}};
        var attn =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}, {AttentionType.N}},
                };
        var rev = buildConstantRevenueMatrix(2, 3, invs, 10.0);
        var problem = buildProblem(comms, invs, suit, attn, rev);
        var solution = buildSolution(problem, new int[][] {{0}, {}, {1}});

        var random = new Random(42);
        var neighborhood = new InterSwapNeighborhood(problem);
        var moveCount = 0;
        for (var unused : neighborhood.generateMoves(solution, random)) {
            moveCount++;
        }

        // Only inv0<->inv2: 1x1 + 1x1 = 2
        assertEquals(2, moveCount);
    }

    @Test
    void typeReturnsInterSwap() {
        var comms = new Commercial[] {new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED)};
        var invs = new Inventory[] {new Inventory(0, 60, 0, 10)};
        var suit = new boolean[][] {{true}};
        var attn = new AttentionType[][][] {{{AttentionType.N}}};
        var rev = buildConstantRevenueMatrix(1, 1, invs, 10.0);
        var problem = buildProblem(comms, invs, suit, attn, rev);

        var neighborhood = new InterSwapNeighborhood(problem);

        assertEquals(NeighborhoodType.INTER_SWAP, neighborhood.type());
    }

    @Test
    void generatedMovesCanBeCheckedAndApplied() {
        var comms =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 100.0, PricingType.FIXED),
                };
        var invs =
                new Inventory[] {
                    new Inventory(0, 60, 0, 10), new Inventory(1, 60, 0, 10),
                };
        var suit = new boolean[][] {{true, true}, {true, true}};
        var attn =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                };
        var rev = buildConstantRevenueMatrix(2, 2, invs, 10.0);
        var problem = buildProblem(comms, invs, suit, attn, rev);
        var solution = buildSolution(problem, new int[][] {{0}, {1}});

        var random = new Random(42);
        var neighborhood = new InterSwapNeighborhood(problem);
        var appliedCount = 0;
        for (var move : neighborhood.generateMoves(solution, random)) {
            if (move.checkFeasibility()) {
                var newSolution = move.apply();
                assertTrue(newSolution.getTotalRevenue() >= 0);
                appliedCount++;
            }
        }

        assertTrue(appliedCount > 0);
    }

    // --- Test helpers (same pattern as InsertNeighborhoodTest) ---

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
            int numComm, int numInv, Inventory[] inventories, double value) {
        var matrix = new double[numComm][numInv][];
        for (int c = 0; c < numComm; c++) {
            for (int i = 0; i < numInv; i++) {
                matrix[c][i] = new double[inventories[i].getDuration()];
                Arrays.fill(matrix[c][i], value);
            }
        }
        return matrix;
    }
}
