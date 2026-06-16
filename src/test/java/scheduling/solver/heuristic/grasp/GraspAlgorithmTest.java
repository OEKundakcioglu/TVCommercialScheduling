package scheduling.solver.heuristic.grasp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.RunInfo;
import scheduling.solver.SolverSolution;
import scheduling.solver.heuristic.grasp.construction.ReactiveAlphaGenerator;
import scheduling.solver.heuristic.grasp.elitepool.ElitePool;
import scheduling.solver.heuristic.grasp.vnd.VND;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.InsertNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.statistics.PhaseStatistics;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;

class GraspAlgorithmTest {

    private static class TestGraspAlgorithm extends GraspAlgorithm {

        TestGraspAlgorithm(GraspConfig config) {
            super(config);
        }

        @Override
        public SolverSolution<GraspInformation> run(Problem problem) {
            return null;
        }

        public GraspSolution exposeBuildInitialSolution(Problem problem, Random random) {
            return buildInitialSolution(problem, random);
        }

        public GraspSolution exposeRunIteration(
                Problem problem,
                ElitePool elitePool,
                ReactiveAlphaGenerator alphaGen,
                VND vnd,
                Random random,
                PhaseStatistics constructionStats,
                PhaseStatistics localSearchStats,
                PhaseStatistics pathRelinkingStats) {
            return runIteration(
                    problem,
                    elitePool,
                    alphaGen,
                    vnd,
                    random,
                    constructionStats,
                    localSearchStats,
                    pathRelinkingStats);
        }

        public double exposeElapsedSeconds(long startTimeMillis) {
            return elapsedSeconds(startTimeMillis);
        }
    }

    private static final Commercial[] COMMERCIALS =
            new Commercial[] {
                new Commercial(0, 0, 0, 30, 10.0, PricingType.FIXED),
                new Commercial(1, 1, 0, 30, 15.0, PricingType.FIXED),
            };

    private Problem buildSmallProblem() {
        var revenueMatrix = new double[2][1][120];
        for (int c = 0; c < 2; c++) {
            for (int t = 0; t < 120; t++) {
                revenueMatrix[c][0][t] = COMMERCIALS[c].getPrice();
            }
        }
        var ratings = new double[1][2][1];
        ratings[0][0][0] = 1.0;
        ratings[0][1][0] = 1.0;
        return new Problem(
                COMMERCIALS,
                new Inventory[] {new Inventory(0, 120, 1, 4)},
                new int[] {1},
                new boolean[][] {{true}, {true}},
                new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}},
                new int[][] {{0}, {0}},
                new int[][] {{0, 1}},
                ratings,
                revenueMatrix);
    }

    private VNDConfig buildVndConfig(Problem problem) {
        List<Neighborhood> neighborhoods = List.of(new InsertNeighborhood(problem));
        return new VNDConfig(
                new FirstImprovingStrategy(), neighborhoods, new SequentialSelector(), 0.0);
    }

    private GraspConfig buildConfig() {
        var vndConfig = buildVndConfig(buildSmallProblem());
        return new GraspConfig(new RunInfo("test", 0), 60, 5, vndConfig, 0.99, 1.01, 100);
    }

    @Test
    void buildInitialSolution_returnsValidSolutionWithNonNegativeRevenue() {
        var algorithm = new TestGraspAlgorithm(buildConfig());
        var problem = buildSmallProblem();

        var solution = algorithm.exposeBuildInitialSolution(problem, new Random(42));

        assertNotNull(solution);
        assertTrue(solution.getTotalRevenue() >= 0.0);
    }

    @Test
    void buildInitialSolution_assignsCommercials() {
        var algorithm = new TestGraspAlgorithm(buildConfig());
        var problem = buildSmallProblem();

        var solution = algorithm.exposeBuildInitialSolution(problem, new Random(42));

        var totalAssigned = 0;
        for (var seq : solution.getSequences()) {
            totalAssigned += seq.length;
        }
        assertTrue(totalAssigned > 0);
    }

    @Test
    void runIteration_returnsSolutionAndAddsToElitePool() {
        var problem = buildSmallProblem();
        var vndConfig = buildVndConfig(problem);
        var config = new GraspConfig(new RunInfo("test", 0), 60, 5, vndConfig, 0.99, 1.01, 100);
        var algorithm = new TestGraspAlgorithm(config);
        var elitePool = new ElitePool(5, problem.getCommercials().length);
        var vnd = new VND(vndConfig, new Random(42));
        var constructionStats = new PhaseStatistics();
        var localSearchStats = new PhaseStatistics();
        var pathRelinkingStats = new PhaseStatistics();

        var result =
                algorithm.exposeRunIteration(
                        problem,
                        elitePool,
                        new ReactiveAlphaGenerator(),
                        vnd,
                        new Random(42),
                        constructionStats,
                        localSearchStats,
                        pathRelinkingStats);

        assertNotNull(result);
        assertTrue(result.getTotalRevenue() >= 0.0);
        assertTrue(elitePool.size() >= 1);
        assertEquals(1, constructionStats.getCallCount());
        assertEquals(1, localSearchStats.getCallCount());
    }

    @Test
    void runIteration_performsPathRelinkingWhenElitePoolHasEnoughSolutions() {
        var problem = buildSmallProblem();
        var numComm = problem.getCommercials().length;
        var elitePool = new ElitePool(5, numComm);
        elitePool.add(buildEliteSolution(numComm, new int[] {0, -1}, 10.0));
        elitePool.add(buildEliteSolution(numComm, new int[] {-1, 0}, 15.0));
        assertEquals(2, elitePool.size());

        var vndConfig = buildVndConfig(problem);
        var pathRelinkingStats = new PhaseStatistics();
        new TestGraspAlgorithm(
                        new GraspConfig(new RunInfo("test", 0), 60, 5, vndConfig, 0.99, 1.01, 100))
                .exposeRunIteration(
                        problem,
                        elitePool,
                        new ReactiveAlphaGenerator(),
                        new VND(vndConfig, new Random(42)),
                        new Random(3),
                        new PhaseStatistics(),
                        new PhaseStatistics(),
                        pathRelinkingStats);

        assertTrue(pathRelinkingStats.getCallCount() >= 1);
    }

    private GraspSolution buildEliteSolution(int numComm, int[] assignedInvId, double revenue) {
        return new GraspSolution(
                new int[][] {{0}},
                new int[][] {{0}},
                new double[][] {{revenue}},
                revenue,
                new int[] {30},
                new int[] {30},
                assignedInvId,
                new int[numComm]);
    }

    @Test
    void createCheckPoint_recordsElapsedTimeCorrectly() {
        var algorithm = new TestGraspAlgorithm(buildConfig());
        var startTime = System.currentTimeMillis() - 2000;

        var checkpoint = algorithm.createCheckPoint(100.0, startTime);

        assertNotNull(checkpoint);
        assertEquals(100.0, checkpoint.getObjective(), 1e-9);
        assertTrue(checkpoint.getTime() >= 1.5);
        assertTrue(checkpoint.getTime() <= 5.0);
    }

    @Test
    void elapsedSeconds_computesCorrectly() {
        var algorithm = new TestGraspAlgorithm(buildConfig());
        var startTime = System.currentTimeMillis() - 1000;

        var elapsed = algorithm.exposeElapsedSeconds(startTime);

        assertTrue(elapsed >= 0.9);
        assertTrue(elapsed <= 3.0);
    }
}
