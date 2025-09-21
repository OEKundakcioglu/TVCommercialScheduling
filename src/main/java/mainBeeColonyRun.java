import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;
import solvers.GlobalRandom;
import solvers.SolverSolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyAlgorithm;
import solvers.heuristicSolvers.beeColonyYu.BeeColonySettings;
import solvers.heuristicSolvers.beeColonyYu.OrienteeringData;
import solvers.heuristicSolvers.beeColonyYu.ReduceProblemToVRP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main class for running Bee Colony Algorithm from command line interface
 */
@Parameters(separators = "=")
public class mainBeeColonyRun {

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
            names = {"--populationSize", "-p"},
            description = "Population size for bee colony",
            required = true)
    private int populationSize;

    @Parameter(
            names = {"--alpha", "-a"},
            description = "Alpha parameter for bee colony algorithm",
            required = true)
    private double alpha;

    @Parameter(
            names = {"--nIter", "-n"},
            description = "Number of iterations parameter",
            required = true)
    private int nIter;

    @Parameter(
            names = {"--T0", "-T"},
            description = "Initial temperature parameter T0",
            required = true)
    private double T0;

    @Parameter(
            names = {"--seed", "-s"},
            description = "Random seed",
            required = true)
    private int seed;

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
        mainBeeColonyRun main = new mainBeeColonyRun();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .programName("mainBeeColonyRun")
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
        if (!new File(instancePath).exists()) {
            throw new IllegalArgumentException("Instance file does not exist: " + instancePath);
        }

        validateParameters();

        if (verbose) {
            System.out.println("=== Bee Colony Algorithm Configuration ===");
            System.out.println("Instance: " + instancePath);
            System.out.println("Output: " + outputPath);
            System.out.println("Time limit: " + timeLimit + " seconds");
            System.out.println("Population size: " + populationSize);
            System.out.println("Alpha: " + alpha);
            System.out.println("nIter: " + nIter);
            System.out.println("T0: " + T0);
            System.out.println("Random seed: " + seed);
            System.out.println("==========================================");
        }

        System.out.println("Loading problem instance...");
        ProblemParameters parameters = new JsonParser().readData(instancePath);

        System.out.println("Reducing problem to VRP...");
        OrienteeringData orienteeringData = ReduceProblemToVRP.reduce(parameters);

        BeeColonySettings beeColonySettings = new BeeColonySettings(
                timeLimit,
                populationSize,
                alpha,
                nIter,
                T0,
                seed,
                instancePath
        );


        var restOfThePath = beeColonySettings.getOutputDirPath(outputPath);
        var outputDir = Paths.get(restOfThePath);

        var logFile = outputDir.resolve("log.txt").toFile();
        var ignored = logFile.getParentFile().mkdirs();

        var outFile = new PrintStream(new FileOutputStream(logFile));
        System.setOut(new PrintStream(outFile));
        System.setErr(new PrintStream(outFile));

        var outputPath = outputDir.resolve("solution.json");

        if (Files.exists(outputPath)) {
            System.out.println("Solution already exists at " + outputPath + ". Skipping execution.");
            return;
        }

        GlobalRandom.init((long) seed);

        System.out.println("Running Bee Colony Algorithm...");
        long startTime = System.currentTimeMillis();

        BeeColonyAlgorithm beeColonyAlgorithm = new BeeColonyAlgorithm(
                orienteeringData,
                beeColonySettings,
                parameters
        );

        SolverSolution solution = beeColonyAlgorithm.getSolverSolution();

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        Utils.feasibilityCheck(solution.getBestSolution());


        Utils.writeObjectToJson(solution, outputPath.toString());

        GlobalRandom.close();
        System.out.println("Bee Colony Algorithm completed successfully!");
    }

    private void validateParameters() {
        if (timeLimit <= 0) {
            throw new IllegalArgumentException("Time limit must be positive");
        }
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive");
        }
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("Alpha must be between 0 and 1");
        }
        if (nIter <= 0) {
            throw new IllegalArgumentException("nIter must be positive");
        }
        if (T0 <= 0) {
            throw new IllegalArgumentException("T0 must be positive");
        }
    }
}