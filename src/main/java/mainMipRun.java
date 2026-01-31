import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;

import runParameters.MipRunSettings;

import solvers.SolverSolution;
import solvers.mipSolvers.BaseModel;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.continuousTimeModel.ContinuousTimeModel;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Main class for running MIP Solver from command line interface */
@Parameters(separators = "=")
public class mainMipRun {

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
            names = {"--modelType", "-m"},
            description = "MIP model type: DISCRETE or CONTINUOUS",
            required = true)
    private String modelType;

    @Parameter(
            names = {"--timeLimit", "-t"},
            description = "Time limit in seconds",
            required = true)
    private int timeLimit;

    @Parameter(
            names = {"--logPath", "-l"},
            description = "Path for MIP solver log file",
            required = true)
    private String logPath;

    @Parameter(
            names = {"--help", "-h"},
            description = "Show help message",
            help = true)
    private boolean help = false;

    @Parameter(
            names = {"--verbose", "-v"},
            description = "Enable verbose output")
    private boolean verbose = false;

    public static void main(String[] args) throws Exception {
        mainMipRun main = new mainMipRun();
        JCommander commander =
                JCommander.newBuilder().addObject(main).programName("mainMipRun").build();

        commander.parse(args);

        if (main.help) {
            commander.usage();
            return;
        }

        main.run();
    }

    private void run() throws Exception {

        if (!new File(instancePath).exists()) {
            throw new IllegalArgumentException("Instance file does not exist: " + instancePath);
        }

        if (!modelType.equalsIgnoreCase("DISCRETE") && !modelType.equalsIgnoreCase("CONTINUOUS")) {
            throw new IllegalArgumentException(
                    "Invalid model type: " + modelType + ". Use DISCRETE or CONTINUOUS.");
        }

        if (verbose) {
            System.out.println("=== MIP Solver Configuration ===");
            System.out.println("Instance: " + instancePath);
            System.out.println("Output: " + outputPath);
            System.out.println("Model type: " + modelType);
            System.out.println("Time limit: " + timeLimit + " seconds");
            System.out.println("Log path: " + logPath);
            System.out.println("================================");
        }

        System.out.println("Loading problem instance...");
        ProblemParameters parameters = new JsonParser().readData(instancePath);

        MipRunSettings mipRunSettings = new MipRunSettings(timeLimit, logPath);

        var outputPath = Paths.get(this.outputPath);
        var instancePath = Paths.get(this.instancePath);
        var instanceName = instancePath.getFileName().toString().replace(".json", "");
        var outputDir = outputPath.resolve(instanceName);

        var logFile = outputDir.resolve("log.txt").toFile();
        var ignored = logFile.getParentFile().mkdirs();

        var outFile = new PrintStream(new FileOutputStream(logFile));
        System.setOut(new PrintStream(outFile));
        System.setErr(new PrintStream(outFile));

        outputPath = outputDir.resolve("solution.json");

        if (Files.exists(outputPath)) {
            System.out.println(
                    "Solution already exists at " + outputPath + ". Skipping execution.");
            return;
        }

        BaseModel model;
        if (modelType.equalsIgnoreCase("DISCRETE")) {
            model = new DiscreteTimeModel(parameters);
        } else {
            model = new ContinuousTimeModel(parameters);
        }

        System.out.println("Running MIP solver...");
        long startTime = System.currentTimeMillis();

        ModelSolver solver = new ModelSolver(model, parameters, mipRunSettings);
        SolverSolution solution = solver.getSolution();

        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;

        Utils.feasibilityCheck(solution.getBestSolution());

        Utils.writeObjectToJson(solution, outputPath.toString());

        System.gc();
        System.out.println("MIP solver completed successfully!");
    }
}
