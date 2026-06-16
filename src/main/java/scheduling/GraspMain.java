package scheduling;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import scheduling.mapping.ProblemDTOReader;
import scheduling.mapping.SolutionDTOWriter;
import scheduling.model.Problem;
import scheduling.model.ProblemBuilder;
import scheduling.solver.FeasibilityCheck;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.HeuristicSolver;
import scheduling.solver.heuristic.grasp.GraspConfig;
import scheduling.solver.heuristic.grasp.MultiThreadGraspAlgorithm;
import scheduling.solver.heuristic.grasp.SingleThreadGraspAlgorithm;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.InsertNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.InterSwapNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.IntraSwapNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.NeighborhoodType;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.OutOfPoolSwapNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.ShiftNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.TransferNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.selector.AdaptiveSelector;
import scheduling.solver.heuristic.grasp.vnd.selector.NeighborhoodSelector;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.selector.ShuffledSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.BestImprovingStrategy;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;
import scheduling.solver.heuristic.grasp.vnd.strategy.SearchStrategy;

@Command(
        name = "grasp",
        mixinStandardHelpOptions = true,
        description = "Run GRASP solver for TV commercial scheduling")
public class GraspMain implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(GraspMain.class);

    @Option(
            names = {"-i", "--instance"},
            required = true,
            description = "Path to problem JSON file")
    private Path instancePath;

    @Option(
            names = {"-o", "--output"},
            required = true,
            description = "Path to write solution JSON file")
    private Path outputPath;

    @Option(
            names = {"-t", "--timeLimit"},
            defaultValue = "60",
            description = "Time limit in seconds (default: ${DEFAULT-VALUE})")
    private int timeLimit;

    @Option(
            names = {"-s", "--seed"},
            defaultValue = "0",
            description = "Random seed (default: ${DEFAULT-VALUE})")
    private int seed;

    @Option(
            names = {"--searchMode"},
            defaultValue = "FIRST_IMPROVING",
            description = "FIRST_IMPROVING or BEST_IMPROVING (default: ${DEFAULT-VALUE})")
    private SearchMode searchMode;

    @Option(
            names = {"--threads"},
            defaultValue = "1",
            description = "Number of threads, 1=single-thread (default: ${DEFAULT-VALUE})")
    private int threads;

    @Option(
            names = {"--elitePoolSize"},
            defaultValue = "10",
            description = "Elite pool size for path relinking (default: ${DEFAULT-VALUE})")
    private int elitePoolSize;

    @Option(
            names = {"-m", "--moves"},
            defaultValue = "INSERT,INTER_SWAP,INTRA_SWAP,OUT_OF_POOL_SWAP,SHIFT,TRANSFER",
            split = ",",
            description = "Comma-separated neighborhood types (default: all)")
    private List<NeighborhoodType> moves;

    @Option(
            names = {"--selectorType"},
            defaultValue = "ADAPTIVE",
            description = "SEQUENTIAL, SHUFFLED, or ADAPTIVE (default: ${DEFAULT-VALUE})")
    private SelectorType selectorType;

    @Option(
            names = {"--skipProbability"},
            defaultValue = "0.0",
            description = "Neighborhood skip probability (default: ${DEFAULT-VALUE})")
    private double skipProbability;

    @Option(
            names = {"--minMoveProbability"},
            defaultValue = "0.05",
            description = "Min probability for adaptive selector (default: ${DEFAULT-VALUE})")
    private double minMoveProbability;

    @Option(
            names = {"--updateEveryNIter"},
            defaultValue = "100",
            description = "Adaptive selector update frequency (default: ${DEFAULT-VALUE})")
    private int updateEveryNIter;

    @Option(
            names = {"--alphaLower"},
            defaultValue = "0.5",
            description = "Alpha lower bound (default: ${DEFAULT-VALUE})")
    private double alphaLower;

    @Option(
            names = {"--alphaUpper"},
            defaultValue = "2.0",
            description = "Alpha upper bound (default: ${DEFAULT-VALUE})")
    private double alphaUpper;

    enum SearchMode {
        FIRST_IMPROVING,
        BEST_IMPROVING,
    }

    enum SelectorType {
        SEQUENTIAL,
        SHUFFLED,
        ADAPTIVE,
    }

    private static final DelegatingOutputStream outDelegate =
            new DelegatingOutputStream(System.out);
    private static final DelegatingOutputStream errDelegate =
            new DelegatingOutputStream(System.err);

    public static void main(String[] args) {
        System.setOut(new PrintStream(outDelegate, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errDelegate, true, StandardCharsets.UTF_8));
        new CommandLine(new GraspMain()).execute(args);
    }

    @Override
    public void run() {
        var problemDTO = ProblemDTOReader.read(instancePath);
        var problem = ProblemBuilder.build(problemDTO);

        var neighborhoods = buildNeighborhoods(problem);
        var selector = buildSelector(neighborhoods);
        var strategy = buildStrategy();
        var vndConfig = new VNDConfig(strategy, neighborhoods, selector, skipProbability);
        var fileName = Objects.requireNonNull(instancePath.getFileName());
        var instanceName = fileName.toString().replaceFirst("\\.[^.]+$", "");
        var runInfo = new RunInfo(instanceName, seed);
        var config =
                new GraspConfig(
                        runInfo,
                        timeLimit,
                        elitePoolSize,
                        vndConfig,
                        alphaLower,
                        alphaUpper,
                        updateEveryNIter);

        var outputDir = config.outputPath(outputPath);
        redirectConsoleOutput(outputDir);

        logConfiguration();
        log.info("Config: {}", config.stringDesc());

        var random = new Random(seed);
        var algorithm =
                threads == 1
                        ? new SingleThreadGraspAlgorithm(config, random)
                        : new MultiThreadGraspAlgorithm(config, random, threads);

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
        log.info("Time limit: {}s | Seed: {} | Threads: {}", timeLimit, seed, threads);
        log.info("Search mode: {} | Selector: {}", searchMode, selectorType);
        log.info("Moves: {}", moves);
        log.info("Elite pool size: {} | Skip probability: {}", elitePoolSize, skipProbability);
        log.info("Alpha: [{}, {}]", alphaLower, alphaUpper);
        if (selectorType == SelectorType.ADAPTIVE) {
            log.info(
                    "Adaptive: minMoveProbability={}, updateEveryNIter={}",
                    minMoveProbability,
                    updateEveryNIter);
        }
    }

    private List<Neighborhood> buildNeighborhoods(Problem problem) {
        return moves.stream().map(type -> buildNeighborhood(type, problem)).toList();
    }

    private Neighborhood buildNeighborhood(NeighborhoodType type, Problem problem) {
        return switch (type) {
            case INSERT -> new InsertNeighborhood(problem);
            case INTER_SWAP -> new InterSwapNeighborhood(problem);
            case INTRA_SWAP -> new IntraSwapNeighborhood(problem);
            case OUT_OF_POOL_SWAP -> new OutOfPoolSwapNeighborhood(problem);
            case SHIFT -> new ShiftNeighborhood(problem);
            case TRANSFER -> new TransferNeighborhood(problem);
        };
    }

    private NeighborhoodSelector buildSelector(List<Neighborhood> neighborhoods) {
        return switch (selectorType) {
            case SEQUENTIAL -> new SequentialSelector();
            case SHUFFLED -> new ShuffledSelector();
            case ADAPTIVE -> new AdaptiveSelector(minMoveProbability, neighborhoods);
        };
    }

    private SearchStrategy buildStrategy() {
        return switch (searchMode) {
            case FIRST_IMPROVING -> new FirstImprovingStrategy();
            case BEST_IMPROVING -> new BestImprovingStrategy();
        };
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
