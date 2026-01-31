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
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorReactive;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

import java.util.List;

enum AlphaGeneratorType {
    REACTIVE,
    UNIFORM,
    CONSTANT
}

public class main {

    // ==================== CONFIGURATION (MODIFY HERE) ====================

    private static final String INSTANCE_PATH =
            "instances/density=HIGH_nInventory=20_nHours=5_seed=1.json";

    private static final int TIME_LIMIT = 300; // seconds
    private static final int SEED = 0;
    private static final int THREAD_COUNT = 0; // 0 = use all available processors

    private static final SearchMode SEARCH_MODE = SearchMode.BEST_IMPROVEMENT;
    private static final List<String> MOVES =
            List.of(
                    "insert", "outOfPool", "transfer", "shift", "interSwap", "intraSwap"
                    //            "chainSwap"
                    );
    private static final double SKIP_PROBABILITY = 0;
    private static final boolean ADAPTIVE_MOVES = true;
    private static final double MIN_MOVE_PROBABILITY = 0.1;
    private static final int UPDATE_EVERY_N_ITER = 100;
    private static final boolean TRACK_STATISTICS = true;

    private static final double CONSTRUCTIVE_DEVIATION = 2;
    private static final int K_REGRET =
            3; // k value for k-regret (only used when CONSTRUCTIVE_TYPE = REGRET_BASED)

    // Alpha Generator Settings
    private static final AlphaGeneratorType ALPHA_TYPE = AlphaGeneratorType.UNIFORM;
    private static final double ALPHA_CONSTANT = 0.4; // for CONSTANT
    private static final double ALPHA_UNIFORM_LOWER = 0.1; // for UNIFORM
    private static final double ALPHA_UNIFORM_UPPER = 0.9; // for UNIFORM

    // =====================================================================

    public static void main(String[] args) throws Exception {
        ProblemParameters problem = new JsonParser().readData(INSTANCE_PATH);
        GraspSettings config = createConfig();

        // Uncomment one of the following:
        //        runSingleThread(problem, config);
        runParallel(problem, config, THREAD_COUNT);
        //        runMIPDiscrete(problem);
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
        var solver = new ModelSolver(model, problem, new MipRunSettings(List.of(7200), ""));
        var solution = solver.getSolution();
    }

    private static GraspSettings createConfig() {
        LocalSearchSettings localSearchSettings =
                new LocalSearchSettings(
                        new java.util.ArrayList<>(MOVES),
                        SKIP_PROBABILITY,
                        ADAPTIVE_MOVES,
                        TRACK_STATISTICS,
                        MIN_MOVE_PROBABILITY,
                        UPDATE_EVERY_N_ITER);

        ConstructiveHeuristicSettings constructiveSettings =
                new ConstructiveHeuristicSettings(0.5, 2);

        return new GraspSettings(
                SEARCH_MODE,
                TIME_LIMIT,
                localSearchSettings,
                constructiveSettings,
                createAlphaGenerator(),
                SEED,
                INSTANCE_PATH);
    }

    private static AlphaGenerator createAlphaGenerator() {
        return switch (ALPHA_TYPE) {
            case REACTIVE -> new AlphaGeneratorReactive();
            case UNIFORM -> new AlphaGeneratorUniform(ALPHA_UNIFORM_LOWER, ALPHA_UNIFORM_UPPER);
            case CONSTANT -> new AlphaGeneratorConstant(ALPHA_CONSTANT);
        };
    }
}
