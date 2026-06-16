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

class ShiftMoveTest {

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
    void feasibleForwardShift() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleBackwardShift() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 2, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void feasibleSamePosition() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(2, 1),
                        buildNAttentionTypes(2, 1),
                        buildConstantRevenueMatrix(2, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1}});
        var move = new ShiftMove(problem, solution, 0, 0, 0);
        assertTrue(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionShiftedCommercialAtNewPosition() {
        // comm0 has F1 at inv0 — must be at position 0.
        // Shifting comm0 from pos 0 to pos 2 violates F1.
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var attentionTypes = buildNAttentionTypes(3, 1);
        attentionTypes[0][0] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionAffectedCommercialForwardShift() {
        // comm2 has L1 at inv0 — must be at last position (pos 2).
        // Forward shift of comm0 from pos 0 to pos 2:
        // new sequence: [1, 2, 0]. comm2 moves from pos 2 to pos 1 → violates L1.
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var attentionTypes = buildNAttentionTypes(3, 1);
        attentionTypes[2][0] = new AttentionType[] {AttentionType.L1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleAttentionAffectedCommercialBackwardShift() {
        // comm0 has F1 at inv0 — must be at position 0.
        // Backward shift of comm2 from pos 2 to pos 0:
        // new sequence: [2, 0, 1]. comm0 moves from pos 0 to pos 1 → violates F1.
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var attentionTypes = buildNAttentionTypes(3, 1);
        attentionTypes[0][0] = new AttentionType[] {AttentionType.F1};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        attentionTypes,
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 2, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAtSourceGap() {
        // Sequence: [0, 1, 2]. comm0 and comm2 have same group (1).
        // Shift comm1 from pos 1 to pos 0 (backward): new sequence [1, 0, 2].
        // Source gap: comm0 (group 1) becomes adjacent to comm2 (group 1) → infeasible.
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 1, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        // Shift comm1 from pos 1 → pos 0. Source gap: comm0 adj comm2 (same group).
        var move = new ShiftMove(problem, solution, 0, 1, 0);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAtDestinationForwardShift() {
        // Sequence: [0, 1, 2, 3]. comm0 and comm2 have same group (1).
        // Forward shift comm1 from pos 1 to pos 2: new sequence [0, 2, 1, 3].
        // Destination: comm2 (group 1) adj to comm1 is fine, but source gap:
        //   comm0 (group 1) adj to comm2 (group 1) → infeasible at source gap.
        // Actually let me design a case that fails at destination specifically.
        // Sequence: [0, 1, 2, 3]. Shift comm0 from pos 0 to pos 2:
        //   new sequence: [1, 2, 0, 3].
        //   Destination: comm2 (left) adj comm0 (middle) adj comm3 (right).
        //   If comm2 and comm0 have same group → infeasible at destination.
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 1, 0, 10, 300.0, PricingType.FIXED),
                    new Commercial(3, 3, 0, 10, 400.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 1),
                        buildNAttentionTypes(4, 1),
                        buildConstantRevenueMatrix(4, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2, 3}});
        // Shift comm0 from pos 0 to pos 2: new [1, 2, 0, 3].
        // comm2 (group 1) adjacent to comm0 (group 1) → infeasible.
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        assertFalse(move.checkFeasibility());
    }

    @Test
    void infeasibleGroupAtDestinationBackwardShift() {
        // Sequence: [0, 1, 2, 3]. Shift comm3 from pos 3 to pos 1:
        //   new sequence: [0, 3, 1, 2].
        //   Destination: comm0 (left) adj comm3 (middle) adj comm1 (right).
        //   If comm0 and comm3 have same group → infeasible.
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED),
                    new Commercial(3, 1, 0, 10, 400.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(4, 1),
                        buildNAttentionTypes(4, 1),
                        buildConstantRevenueMatrix(4, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2, 3}});
        // Shift comm3 from pos 3 to pos 1: new [0, 3, 1, 2].
        // comm0 (group 1) adjacent to comm3 (group 1) → infeasible.
        var move = new ShiftMove(problem, solution, 0, 3, 1);
        assertFalse(move.checkFeasibility());
    }

    // --- calculateRevenueGain ---

    @Test
    void revenueGainZeroWithConstantRevenue() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 10, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 10, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainForwardShiftWithTimeVaryingRevenue() {
        // rev(c, inv, t) = (c+1)*100 + t
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var revenueMatrix = new double[3][1][];
        for (int c = 0; c < 3; c++) {
            revenueMatrix[c][0] = new double[121];
            for (int t = 0; t < 121; t++) {
                revenueMatrix[c][0][t] = (c + 1) * 100.0 + t;
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        revenueMatrix);
        // Sequence: [0(dur=10), 1(dur=20), 2(dur=15)]
        // StartTimes: [0, 10, 30]
        // Revenues: [100, 210, 330]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});

        // Forward shift comm0 from pos 0 to pos 2: new sequence [1, 2, 0]
        // New start times: [0, 20, 35]
        // comm1: was at t=10 rev=210, now at t=0 rev=200. delta=-10
        // comm2: was at t=30 rev=330, now at t=20 rev=320. delta=-10
        // comm0: was at t=0 rev=100, now at t=35 rev=135. delta=+35
        // total gain = 15
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        assertEquals(15.0, move.calculateRevenueGain(), 1e-6);
    }

    @Test
    void revenueGainBackwardShiftWithTimeVaryingRevenue() {
        // rev(c, inv, t) = (c+1)*100 + t
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var revenueMatrix = new double[3][1][];
        for (int c = 0; c < 3; c++) {
            revenueMatrix[c][0] = new double[121];
            for (int t = 0; t < 121; t++) {
                revenueMatrix[c][0][t] = (c + 1) * 100.0 + t;
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        revenueMatrix);
        // Sequence: [0(dur=10), 1(dur=20), 2(dur=15)]
        // StartTimes: [0, 10, 30]
        // Revenues: [100, 210, 330]
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});

        // Backward shift comm2 from pos 2 to pos 0: new sequence [2, 0, 1]
        // New start times: [0, 15, 25]
        // comm0: was at t=0 rev=100, now at t=15 rev=115. delta=+15
        // comm1: was at t=10 rev=210, now at t=25 rev=225. delta=+15
        // comm2: was at t=30 rev=330, now at t=0 rev=300. delta=-30
        // total gain = 0
        var move = new ShiftMove(problem, solution, 0, 2, 0);
        assertEquals(0.0, move.calculateRevenueGain(), 1e-6);
    }

    // --- apply ---

    @Test
    void applyForwardShiftRearrangesSequence() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var result = new ShiftMove(problem, solution, 0, 0, 2).apply();
        assertArrayEquals(new int[] {1, 2, 0}, result.getSequences()[0]);
    }

    @Test
    void applyBackwardShiftRearrangesSequence() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var result = new ShiftMove(problem, solution, 0, 2, 0).apply();
        assertArrayEquals(new int[] {2, 0, 1}, result.getSequences()[0]);
    }

    @Test
    void applyRebuildsStartTimes() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        // Forward shift comm0(dur=10) from pos 0 to pos 2: [1(dur=20), 2(dur=15), 0(dur=10)]
        // Start times: [0, 20, 35]
        var result = new ShiftMove(problem, solution, 0, 0, 2).apply();
        assertArrayEquals(new int[] {0, 20, 35}, result.getStartTimes()[0]);
    }

    @Test
    void applyDoesNotMutateOriginal() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var origSeq = solution.getSequences()[0].clone();
        var origStartTimes = solution.getStartTimes()[0].clone();
        final var origTotalRevenue = solution.getTotalRevenue();
        new ShiftMove(problem, solution, 0, 0, 2).apply();
        assertArrayEquals(origSeq, solution.getSequences()[0]);
        assertArrayEquals(origStartTimes, solution.getStartTimes()[0]);
        assertEquals(origTotalRevenue, solution.getTotalRevenue(), 1e-6);
    }

    @Test
    void applyPreservesTotalInvDurationAndHourlyDuration() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var result = new ShiftMove(problem, solution, 0, 0, 2).apply();
        assertEquals(solution.getTotalInvDuration()[0], result.getTotalInvDuration()[0]);
        assertArrayEquals(solution.getTotalDurationOfHour(), result.getTotalDurationOfHour());
    }

    @Test
    void applyTotalRevenueMatchesGain() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var revenueMatrix = new double[3][1][];
        for (int c = 0; c < 3; c++) {
            revenueMatrix[c][0] = new double[121];
            for (int t = 0; t < 121; t++) {
                revenueMatrix[c][0][t] = (c + 1) * 100.0 + t;
            }
        }
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        revenueMatrix);
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        var result = move.apply();
        assertEquals(
                solution.getTotalRevenue() + move.calculateRevenueGain(),
                result.getTotalRevenue(),
                1e-6);
    }

    @Test
    void applyPassesFeasibilityCheck() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED),
                    new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED),
                    new Commercial(2, 3, 0, 15, 300.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 120, 0, 5)};
        var problem =
                buildProblem(
                        commercials,
                        inventories,
                        buildAllSuitable(3, 1),
                        buildNAttentionTypes(3, 1),
                        buildConstantRevenueMatrix(3, 1, inventories));
        var solution = buildCheckedSolution(problem, new int[][] {{0, 1, 2}});
        var move = new ShiftMove(problem, solution, 0, 0, 2);
        var result = move.apply();
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
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
                    if (solution.getSequences()[inv].length >= 2) {
                        candidateInvs.add(inv);
                    }
                }
                if (candidateInvs.isEmpty()) {
                    break;
                }
                var invId = candidateInvs.get(rng.nextInt(candidateInvs.size()));
                var seqLen = solution.getSequences()[invId].length;
                var fromPos = rng.nextInt(seqLen);
                var toPos = rng.nextInt(seqLen);
                if (fromPos == toPos) {
                    continue;
                }

                var move = new ShiftMove(problem, solution, invId, fromPos, toPos);
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
                            "checkFeasibility=true but FeasibilityCheck failed at trial="
                                    + trial
                                    + " attempt="
                                    + attempt);
                } else {
                    infeasibleCount++;
                    assertThrows(
                            IllegalStateException.class,
                            () -> FeasibilityCheck.check(problem, applied),
                            "checkFeasibility=false but FeasibilityCheck passed at trial="
                                    + trial
                                    + " attempt="
                                    + attempt);
                }
            }
        }

        assertTrue(feasibleCount > 0, "No feasible moves were tested");
        assertTrue(infeasibleCount > 0, "No infeasible moves were tested");
    }
}
