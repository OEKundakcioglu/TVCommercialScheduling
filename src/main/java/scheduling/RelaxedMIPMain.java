package scheduling;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import scheduling.mapping.ProblemDTOReader;
import scheduling.mapping.SolutionDTOWriter;
import scheduling.model.ProblemBuilder;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.mip.RelaxedMIPConfig;
import scheduling.solver.mip.RelaxedMIPReturnMode;
import scheduling.solver.mip.RelaxedMIPSolver;
import scheduling.solver.mip.model.RelaxedMIPModel;

@Command(
        name = "relaxed-mip",
        mixinStandardHelpOptions = true,
        description = "Run relaxed MIP upper-bound solver for TV commercial scheduling")
public class RelaxedMIPMain implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RelaxedMIPMain.class);

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
            names = {"--returnMode"},
            defaultValue = "BEST",
            description = "BEST, AVERAGE, or WORST break return coefficient")
    private RelaxedMIPReturnMode returnMode;

    private static final DelegatingOutputStream outDelegate =
            new DelegatingOutputStream(System.out);
    private static final DelegatingOutputStream errDelegate =
            new DelegatingOutputStream(System.err);

    public static void main(String[] args) {
        System.setOut(new PrintStream(outDelegate, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errDelegate, true, StandardCharsets.UTF_8));
        new CommandLine(new RelaxedMIPMain()).execute(args);
    }

    @Override
    public void run() {
        var problemDTO = ProblemDTOReader.read(instancePath);
        var problem = ProblemBuilder.build(problemDTO);

        var fileName = Objects.requireNonNull(instancePath.getFileName());
        var instanceName = fileName.toString().replaceFirst("\\.[^.]+$", "");
        var config = new RelaxedMIPConfig(instanceName, returnMode);

        var outputDir = config.outputPath(outputPath);
        redirectConsoleOutput(outputDir);

        logConfiguration();
        log.info("Config: {}", config.stringDesc());

        var model = new RelaxedMIPModel(config);
        var solver = new RelaxedMIPSolver(model);
        var result = solver.solve(problem);

        FeasibilityCheck.check(problem, result.getBestSolution());

        var solutionPath = outputDir.resolve("solution.json");
        SolutionDTOWriter.write(result, solutionPath);

        var info = result.getAdditionalInformation();
        System.out.println("Best revenue: " + result.getBestSolution().getTotalRevenue());
        System.out.println("Relaxed upper bound: " + info.relaxedUpperBound());
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
        log.info("MIP gap: {}", RelaxedMIPConfig.DEFAULT_MIP_GAP);
        log.info("Return mode: {}", returnMode);
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
