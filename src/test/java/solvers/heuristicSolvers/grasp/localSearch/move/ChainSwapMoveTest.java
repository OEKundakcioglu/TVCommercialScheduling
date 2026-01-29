package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import data.problemBuilders.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.LoopSetup;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainSwapMoveTest {

    private ProblemParameters problem;
    private Solution solution;
    private double[] hourlyDurations;
    private Random random;
    private ConstructiveHeuristicSettings settings;

    @BeforeEach
    void setUp() throws Exception {
        // Load problem instance
        problem = new JsonParser().readData(
                "instances/density=LOW_nInventory=10_nHours=3_seed=1.json");

        // Set static config
        LoopSetup.numberOfCommercials = problem.getSetOfCommercials().size();
        LoopSetup.isDebug = true;

        // Create settings for constructive heuristic
        settings = new ConstructiveHeuristicSettings(0.8, 1.2);
        random = new Random(42);

        // Build valid solution using constructive heuristic
        solution = new ConstructiveHeuristic(problem, 0.2, settings, random).getSolution();

        // Calculate hourly durations
        hourlyDurations = Utils.getHourlyDurations(solution, problem);
    }

    /**
     * Picks 3 random inventories that have commercials and random positions within each.
     * Returns null if we can't find 3 distinct inventories with commercials.
     */
    private ChainSwapMove createRandomChainSwapMove(Solution sol, Random rnd) {
        // Find inventories that have at least one commercial
        List<Inventory> inventoriesWithCommercials = new ArrayList<>();
        for (Inventory inv : problem.getSetOfInventories()) {
            List<SolutionData> invSolution = sol.solution.get(inv.getId());
            if (invSolution != null && !invSolution.isEmpty()) {
                inventoriesWithCommercials.add(inv);
            }
        }

        // Need at least 3 distinct inventories
        if (inventoriesWithCommercials.size() < 3) {
            return null;
        }

        // Pick 3 random distinct inventories
        List<Inventory> shuffled = new ArrayList<>(inventoriesWithCommercials);
        java.util.Collections.shuffle(shuffled, rnd);

        Inventory i1 = shuffled.get(0);
        Inventory i2 = shuffled.get(1);
        Inventory i3 = shuffled.get(2);

        // Pick random positions in each inventory
        List<SolutionData> list1 = sol.solution.get(i1.getId());
        List<SolutionData> list2 = sol.solution.get(i2.getId());
        List<SolutionData> list3 = sol.solution.get(i3.getId());

        int n1 = rnd.nextInt(list1.size());
        int n2 = rnd.nextInt(list2.size());
        int n3 = rnd.nextInt(list3.size());

        // Get current hourly durations
        double[] currentHourlyDurations = Utils.getHourlyDurations(sol, problem);

        return new ChainSwapMove(sol, i1, n1, i2, n2, i3, n3, currentHourlyDurations);
    }

    @RepeatedTest(100)
    void testRandomChainSwapsPreserveFeasibility() throws Exception {
        // Create new random for this test iteration
        Random testRandom = new Random();

        ChainSwapMove move = createRandomChainSwapMove(solution, testRandom);
        if (move == null) {
            // Skip if we couldn't create a move (not enough inventories with commercials)
            return;
        }

        // If checkFeasibility returns true, apply the move and verify feasibility
        if (move.checkFeasibility()) {
            Solution newSolution = move.applyMove();

            // Utils.feasibilityCheck throws an exception if infeasible
            // No exception means the solution is feasible
            assertDoesNotThrow(() -> Utils.feasibilityCheck(newSolution),
                    "Feasible move should produce a feasible solution");
        }
    }

    @RepeatedTest(100)
    void testRevenueGainMatchesSolutionDifference() throws Exception {
        // Create new random for this test iteration
        Random testRandom = new Random();

        ChainSwapMove move = createRandomChainSwapMove(solution, testRandom);
        if (move == null) {
            return;
        }

        if (move.checkFeasibility()) {
            int originalRevenue = solution.revenue;
            double gain = move.calculateRevenueGain();

            Solution newSolution = move.applyMove();
            int expectedRevenue = originalRevenue + (int) gain;

            assertEquals(expectedRevenue, newSolution.revenue,
                    "New solution revenue should equal original revenue + calculated gain");
        }
    }

    @RepeatedTest(100)
    void testApplyMoveDoesNotModifyOriginal() throws Exception {
        // Create new random for this test iteration
        Random testRandom = new Random();

        ChainSwapMove move = createRandomChainSwapMove(solution, testRandom);
        if (move == null) {
            return;
        }

        if (move.checkFeasibility()) {
            // Capture original state
            int originalRevenue = solution.revenue;
            List<List<Integer>> originalCommercialIds = new ArrayList<>();
            for (List<SolutionData> invSolution : solution.solution) {
                List<Integer> ids = new ArrayList<>();
                for (SolutionData sd : invSolution) {
                    ids.add(sd.getCommercial().getId());
                }
                originalCommercialIds.add(ids);
            }

            // Apply move
            move.applyMove();

            // Verify original is unchanged
            assertEquals(originalRevenue, solution.revenue,
                    "Original solution revenue should not change after applyMove");

            for (int i = 0; i < solution.solution.size(); i++) {
                List<SolutionData> invSolution = solution.solution.get(i);
                List<Integer> originalIds = originalCommercialIds.get(i);
                assertEquals(originalIds.size(), invSolution.size(),
                        "Original inventory " + i + " should have same size after applyMove");
                for (int j = 0; j < invSolution.size(); j++) {
                    assertEquals(originalIds.get(j), invSolution.get(j).getCommercial().getId(),
                            "Original inventory " + i + " commercial at position " + j + " should be unchanged");
                }
            }
        }
    }

    @RepeatedTest(100)
    void testFeasibilityCheckConsistency() throws Exception {
        // Create new random for this test iteration
        Random testRandom = new Random();

        ChainSwapMove move = createRandomChainSwapMove(solution, testRandom);
        if (move == null) {
            return;
        }

        boolean isFeasible = move.checkFeasibility();

        if (isFeasible) {
            // If checkFeasibility returns true, applyMove should succeed without exception
            assertDoesNotThrow(() -> move.applyMove(),
                    "If checkFeasibility() returns true, applyMove() should succeed");
        }
        // Note: If checkFeasibility returns false, we don't test applyMove
        // because it's expected to potentially fail or produce an infeasible solution
    }
}
