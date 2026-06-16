package scheduling.solver.heuristic.grasp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class SolutionConverterTest {

    @Test
    void convertsGraspSolutionToSolution() {
        // revenueMatrix[commId][invId][startTime]
        // comm0 duration=10, FIXED => price*duration = 100*10 = 1000 regardless of rating
        // comm1 duration=20, FIXED => price*duration = 200*20 = 4000
        var revenueMatrix = new double[2][2][];
        revenueMatrix[0][0] = new double[121]; // comm0 in inv0
        revenueMatrix[1][0] = new double[121]; // comm1 in inv0
        revenueMatrix[0][1] = new double[121]; // comm0 in inv1
        revenueMatrix[1][1] = new double[121]; // comm1 in inv1
        for (int t = 0; t < 121; t++) {
            revenueMatrix[0][0][t] = 1000.0;
            revenueMatrix[1][0][t] = 4000.0;
            revenueMatrix[0][1][t] = 1000.0;
            revenueMatrix[1][1][t] = 4000.0;
        }

        var comm0 = new Commercial(0, 1, 0, 10, 100.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 200.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 120, 1, 5);
        var inv1 = new Inventory(1, 120, 2, 5);
        var problem =
                new Problem(
                        new Commercial[] {comm0, comm1},
                        new Inventory[] {inv0, inv1},
                        new int[] {1, 2},
                        new boolean[][] {{true, true}, {true, true}},
                        new AttentionType[][][] {
                            {{AttentionType.N}, {AttentionType.N}},
                            {{AttentionType.N}, {AttentionType.N}}
                        },
                        new int[][] {{0, 1}, {0, 1}},
                        new int[][] {{0, 1}, {0, 1}},
                        new double[][][] {{{0.0}}, {{0.0}}},
                        revenueMatrix);

        // inv0 has [comm0, comm1], inv1 is empty
        var sequences = new int[][] {{0, 1}, {}};
        var startTimes = new int[][] {{0, 10}, {}};
        var revenues = new double[][] {{1000.0, 4000.0}, {}};
        var totalDurationOfHour = new int[] {30, 0};
        var totalInvDuration = new int[] {30, 0};
        var graspSolution =
                new GraspSolution(
                        sequences,
                        startTimes,
                        revenues,
                        5000.0,
                        totalDurationOfHour,
                        totalInvDuration,
                        new int[] {0, 0},
                        new int[] {0, 1});

        var solution = SolutionConverter.toSolution(problem, graspSolution);

        // Check assignments
        var inv0Assignments = solution.getAssignments().get(inv0);
        assertEquals(2, inv0Assignments.size());
        assertEquals(comm0, inv0Assignments.get(0));
        assertEquals(comm1, inv0Assignments.get(1));
        assertTrue(solution.getAssignments().get(inv1).isEmpty());

        // Revenue recomputed from scratch: comm0 at t=0 -> 1000, comm1 at t=10 -> 4000
        assertEquals(5000.0, solution.getTotalRevenue(), 1e-6);
    }

    @Test
    void handlesEmptySolution() {
        var inv0 = new Inventory(0, 120, 1, 5);
        var problem =
                new Problem(
                        new Commercial[] {},
                        new Inventory[] {inv0},
                        new int[] {1},
                        new boolean[][] {},
                        new AttentionType[][][] {},
                        new int[][] {},
                        new int[][] {{}},
                        new double[][][] {{{0.0}}},
                        new double[][][] {});

        var graspSolution =
                new GraspSolution(
                        new int[][] {{}},
                        new int[][] {{}},
                        new double[][] {{}},
                        0.0,
                        new int[] {0},
                        new int[] {0},
                        new int[0],
                        new int[0]);

        var solution = SolutionConverter.toSolution(problem, graspSolution);

        assertTrue(solution.getAssignments().get(inv0).isEmpty());
        assertEquals(0.0, solution.getTotalRevenue(), 1e-6);
    }
}
