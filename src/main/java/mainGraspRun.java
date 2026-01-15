import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import solvers.GlobalRandom;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Main class for running GRASP algorithm from command line interface
 * Usage: java mainGraspRun --instancePath=path/to/instance.json [options]
 */
@Parameters(separators = "=")
public class mainGraspRun {

    @Parameter(
            names = {"--instancePath", "-i"},
            description = "Path to the problem instance JSON file",
            required = true)
    private String instancePath;

    @Parameter(
            names = {"--outputPath", "-o"},
            description = "Path to save the solution JSON file",
            required = true)
    private String outputPath;

    @Parameter(
            names = {"--timeLimit", "-t"},
            description = "Time limit in seconds",
            required = true)
    private int timeLimit;

    @Parameter(
            names = {"--searchMode", "-s"},
            description = "Local search mode: BEST_IMPROVEMENT or FIRST_IMPROVEMENT",
            required = true)
    private String searchMode;

    @Parameter(
            names = {"--alphaType", "-at"},
            description = "Alpha generator type: FIXED or UNIFORM",
            required = true)
    private String alphaType;

    @Parameter(
            names = {"--alpha", "-a"},
            description = "Alpha value for FIXED type (required when alphaType=FIXED)")
    private Double alpha;

    @Parameter(
            names = {"--minAlpha", "-min"},
            description = "Minimum alpha value for UNIFORM type (required when alphaType=UNIFORM)")
    private Double minAlpha;

    @Parameter(
            names = {"--maxAlpha", "-max"},
            description = "Maximum alpha value for UNIFORM type (required when alphaType=UNIFORM)")
    private Double maxAlpha;

    @Parameter(
            names = {"--skipProbability", "-sp"},
            description = "Neighborhood skip probability",
            required = true)
    private double skipProbability;

    @Parameter(
            names = {"--seed", "-seed"},
            description = "Random seed",
            required = true)
    private int seed;

    @Parameter(
            names = {"--moves", "-m"},
            description = "Comma-separated list of local search moves to use",
            required = true)
    private List<String> moves;

    @Parameter(
            names = {"--help", "-h"},
            description = "Show help message",
            help = true)
    private boolean help = false;

    @Parameter(
            names = {"--verbose", "-v"},
            description = "Enable verbose output")
    private boolean verbose = false;

    public static void main(String[] args) {
        mainGraspRun main = new mainGraspRun();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .programName("mainGraspRun")
                .build();

        try {
            commander.parse(args);

            if (main.help) {
                commander.usage();
                return;
            }

            main.run();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            commander.usage();
            System.exit(1);
        }
    }

    private void run() throws Exception {
        if (verbose) {
            System.out.println("=== GRASP Algorithm Configuration ===");
            System.out.println("Instance: " + instancePath);
            System.out.println("Output Dir: " + outputPath);
            System.out.println("Time limit: " + timeLimit + " seconds");
            System.out.println("Search mode: " + searchMode);
            System.out.println("Alpha type: " + alphaType);
            if (alphaType.equalsIgnoreCase("FIXED")) {
                System.out.println("Alpha value: " + alpha);
            } else {
                System.out.println("Alpha range: [" + minAlpha + ", " + maxAlpha + "]");
            }
            System.out.println("Skip probability: " + skipProbability);
            System.out.println("Random seed: " + seed);
            System.out.println("=====================================");
        }

        // Load problem instance
        System.out.println("Loading problem instance...");
        ProblemParameters parameters = new JsonParser().readData(instancePath);

        // Initialize global random with seed
        GlobalRandom.init((long) seed);

        // Create GRASP settings
        GraspSettings graspSettings = createGraspSettings();

        var outputPath = Paths.get(this.outputPath);
        var instancePath = Paths.get(this.instancePath);
        var instanceName = instancePath.getFileName().toString().replace(".json", "");
        outputPath = outputPath.resolve(instanceName);

        var restOfThePath = graspSettings.getStringIdentifier();
        var outputDir = outputPath.resolve(restOfThePath);

        var logFile = outputDir.resolve("log.txt").toFile();
        var ignored = logFile.getParentFile().mkdirs();

        var outFile = new PrintStream(new FileOutputStream(logFile));
        System.setOut(new PrintStream(outFile));
        System.setErr(new PrintStream(outFile));

        outputPath = outputDir.resolve("solution.json");

        if (Files.exists(outputPath)) {
            System.out.println("Solution already exists at " + outputPath + ". Skipping execution.");
            return;
        }

        // Run GRASP algorithm
        System.out.println("Running GRASP algorithm...");
        long startTime = System.currentTimeMillis();

        SolverSolution solution = new GraspWithPathRelinking(parameters, graspSettings).getSolution();

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        // Validate solution
        Utils.feasibilityCheck(solution.getBestSolution());

        // Save solution
        Utils.writeObjectToJson(solution, outputPath.toString());

        // Cleanup
        GlobalRandom.close();

        System.out.println("GRASP algorithm completed successfully!");
    }

    private GraspSettings createGraspSettings() {
        // Create alpha generator
        AlphaGenerator alphaGenerator;
        if (alphaType.equalsIgnoreCase("FIXED")) {
            alphaGenerator = new AlphaGeneratorConstant(alpha);
        } else {
            alphaGenerator = new AlphaGeneratorUniform(minAlpha, maxAlpha);
        }

        // Create search mode
        SearchMode mode;
        try {
            mode = SearchMode.valueOf(searchMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid search mode: " + searchMode + ". Use BEST_IMPROVEMENT or FIRST_IMPROVEMENT.");
        }

        // Create local search settings with default moves
        LocalSearchSettings localSearchSettings = new LocalSearchSettings(moves, skipProbability);

        // Create constructive heuristic settings with default values
        ConstructiveHeuristicSettings constructiveSettings = new ConstructiveHeuristicSettings(0.5, 2);

        // Create and return GRASP settings
        return new GraspSettings(
                mode,
                timeLimit,
                localSearchSettings,
                constructiveSettings,
                alphaGenerator,
                seed,
                instancePath
        );
    }
}