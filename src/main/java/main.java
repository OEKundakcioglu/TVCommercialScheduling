import com.gurobi.gurobi.GRBException;

import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;

import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import runParameters.MipRunSettings;

import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.ParallelGraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;
import solvers.pipeline.GraspMipPipeLine;
import solvers.pipeline.PipelineConfig;

import java.util.List;

public class main {

    // ==================== CONFIGURATION (MODIFY HERE) ====================

    private static final String INSTANCE_PATH =
            "instances/density=HIGH_nInventory=20_nHours=5_seed=1.json";

    private static final int TIME_LIMIT = 60; // seconds
    private static final int SEED = 0;
    private static final int THREAD_COUNT = 0; // 0 = use all available processors

    private static final SearchMode SEARCH_MODE = SearchMode.BEST_IMPROVEMENT;
    private static final List<String> MOVES =
            List.of("insert", "outOfPool", "transfer", "shift", "interSwap", "intraSwap");
    private static final double SKIP_PROBABILITY = 0;
    private static final double MIN_MOVE_PROBABILITY = 0.1;
    private static final int UPDATE_EVERY_N_ITER = 100;

    private static final double CONSTRUCTIVE_DEVIATION = 2;
    private static final int K_REGRET =
            3; // k value for k-regret (only used when CONSTRUCTIVE_TYPE = REGRET_BASED)

    // =====================================================================

    public static void main(String[] args) throws Exception {
        ProblemParameters problem = new JsonParser().readData(INSTANCE_PATH);
        GraspSettings config = createConfig();

        // Uncomment one of the following:
        //        runSingleThread(problem, config);
        //        runParallel(problem, config, THREAD_COUNT);
        //        runMIPDiscrete(problem);
        runWithPipeline(problem);
    }

    private static void runSingleThread(ProblemParameters problem, GraspSettings config)
            throws Exception {
        System.out.println("Running single-threaded GRASP with Path Relinking...");
        System.out.println("Instance: " + INSTANCE_PATH);
        System.out.println("Time limit: " + TIME_LIMIT + "s");

        long startTime = System.currentTimeMillis();
        GraspWithPathRelinking grasp = new GraspWithPathRelinking(problem, config);
        long endTime = System.currentTimeMillis();

        SolverSolution solution = grasp.getSolution();

        System.out.println("\n=== Results ===");
        System.out.println("Final revenue: " + solution.getBestSolution().revenue);
        System.out.println("Total time: " + (endTime - startTime) / 1000.0 + "s");
        System.out.println("Checkpoints: " + solution.getCheckPoints().size());

        Utils.feasibilityCheck(solution.getBestSolution());
    }

    private static void runParallel(
            ProblemParameters problem, GraspSettings config, int threadCount) throws Exception {
        System.out.println("Running parallel GRASP with Path Relinking...");
        System.out.println("Instance: " + INSTANCE_PATH);
        System.out.println("Time limit: " + TIME_LIMIT + "s");

        long startTime = System.currentTimeMillis();
        ParallelGraspWithPathRelinking grasp =
                new ParallelGraspWithPathRelinking(problem, config, threadCount);
        long endTime = System.currentTimeMillis();

        SolverSolution solution = grasp.getSolution();

        System.out.println("\n=== Results ===");
        System.out.println("Final revenue: " + solution.getBestSolution().revenue);
        System.out.println("Total time: " + (endTime - startTime) / 1000.0 + "s");
        System.out.println("Checkpoints: " + solution.getCheckPoints().size());

        Utils.feasibilityCheck(solution.getBestSolution());
    }

    public static void runMIPDiscrete(ProblemParameters problem) throws GRBException {
        var model = new DiscreteTimeModel(problem);
        var solver = new ModelSolver(model, problem, new MipRunSettings(1400, ""));
        var solution = solver.getSolution();
    }

    private static GraspSettings createConfig() {
        LocalSearchSettings localSearchSettings =
                new LocalSearchSettings(
                        new java.util.ArrayList<>(MOVES),
                        SKIP_PROBABILITY,
                        MIN_MOVE_PROBABILITY,
                        UPDATE_EVERY_N_ITER);

        ConstructiveHeuristicSettings constructiveSettings =
                new ConstructiveHeuristicSettings(0.5, 2);

        return new GraspSettings(
                SEARCH_MODE,
                TIME_LIMIT,
                localSearchSettings,
                constructiveSettings,
                new AlphaGeneratorReactive(),
                SEED,
                INSTANCE_PATH);
    }

    public static void runWithPipeline(ProblemParameters problem) throws Exception {
        var graspSettings = createConfig();
        var mipSettings = new MipRunSettings(240, "");

        var pipelineConfig = new PipelineConfig(graspSettings, mipSettings);
        var pipeline = new GraspMipPipeLine(problem, pipelineConfig);
        pipeline.solve();
    }
}
