package scheduling;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import scheduling.mapping.ProblemDTOReader;
import scheduling.mapping.SolutionDTOWriter;
import scheduling.model.ProblemBuilder;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.HeuristicSolver;
import scheduling.solver.heuristic.beecolony.BeeColonyAlgorithm;
import scheduling.solver.heuristic.beecolony.BeeColonyConfig;

@Command(
        name = "beecolony",
        mixinStandardHelpOptions = true,
        description = "Run Bee Colony solver for TV commercial scheduling")
public class BeeColonyMain implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BeeColonyMain.class);

    @Option(
            names = {"-i", "--instance"},
            required = true,
            description = "Path to problem JSON file")
    private Path instancePath;

    @Option(
            names = {"-o", "--output"},
            required = true,
            description = "Output directory for solution files")
    private Path outputPath;

    @Option(
            names = {"-s", "--seed"},
            required = true,
            description = "Random seed")
    private int seed;

    @Option(
            names = {"-t", "--timeLimit"},
            required = true,
            description = "Time limit in seconds")
    private int timeLimit;

    @Option(
            names = {"--populationSize"},
            required = true,
            description = "Bee colony population size")
    private int populationSize;

    @Option(
            names = {"--initialTemperature"},
            required = true,
            description = "Initial temperature for SA acceptance")
    private double initialTemperature;

    @Option(
            names = {"--coolingCoefficient"},
            required = true,
            description = "Cooling coefficient for SA acceptance")
    private double coolingCoefficient;

    @Option(
            names = {"--coolingInterval"},
            required = true,
            description = "Generations between cooling steps")
    private int coolingInterval;

    private static final DelegatingOutputStream outDelegate =
            new DelegatingOutputStream(System.out);
    private static final DelegatingOutputStream errDelegate =
            new DelegatingOutputStream(System.err);

    public static void main(String[] args) {
        System.setOut(new PrintStream(outDelegate, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errDelegate, true, StandardCharsets.UTF_8));
        new CommandLine(new BeeColonyMain()).execute(args);
    }

    @Override
    public void run() {
        var problemDTO = ProblemDTOReader.read(instancePath);
        var problem = ProblemBuilder.build(problemDTO);

        var fileName = Objects.requireNonNull(instancePath.getFileName());
        var instanceName = fileName.toString().replaceFirst("\\.[^.]+$", "");
        var runInfo = new RunInfo(instanceName, seed);
        var config =
                new BeeColonyConfig(
                        runInfo,
                        populationSize,
                        initialTemperature,
                        coolingCoefficient,
                        coolingInterval,
                        timeLimit);

        var outputDir = config.outputPath(outputPath);
        redirectConsoleOutput(outputDir);

        logConfiguration();
        log.info("Config: {}", config.stringDesc());

        var random = new Random(seed);
        var algorithm = new BeeColonyAlgorithm(config, random);
        var solver = new HeuristicSolver<>(algorithm);
        var result = solver.solve(problem);

        FeasibilityCheck.check(problem, result.getBestSolution());

        var solutionPath = outputDir.resolve("solution.json");
        SolutionDTOWriter.write(result, solutionPath);

        System.out.println("Best revenue: " + result.getBestSolution().getTotalRevenue());
    }

    private static void redirectConsoleOutput(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
            var fileOut = new FileOutputStream(outputDir.resolve("console.log").toFile());
            outDelegate.setDelegate(fileOut);
            errDelegate.setDelegate(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Failed to redirect console output", e);
        }
    }

    private void logConfiguration() {
        log.info("Instance: {}", instancePath);
        log.info("Output: {}", outputPath);
        log.info("Time limit: {}s | Seed: {}", timeLimit, seed);
        log.info(
                "Population: {} | Temperature: {} | Cooling: {} (every {} generations)",
                populationSize,
                initialTemperature,
                coolingCoefficient,
                coolingInterval);
    }

    private static class DelegatingOutputStream extends OutputStream {
        private volatile OutputStream delegate;

        DelegatingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        void setDelegate(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }
    }
}
