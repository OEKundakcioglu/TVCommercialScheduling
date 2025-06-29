import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;

import org.yaml.snakeyaml.Yaml;

import runParameters.*;

import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.heuristicSolvers.grasp.localSearch.SearchMode;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGenerator;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorConstant;
import solvers.heuristicSolvers.grasp.reactiveGrasp.AlphaGeneratorUniform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Parameters(separators = "=")
public class mainConsoleLoop {
    @Parameter(
            names = {"--yamlConfigPath", "--ycp"},
            description = "Path to the yaml configuration file.",
            required = true)
    private String yamlPath;

    public static void main(String[] args) throws Exception {
        mainConsoleLoop main = new mainConsoleLoop();
        JCommander commander = JCommander.newBuilder().addObject(main).build();
        commander.parse(args);

        var consoleConfigLoopPath = main.yamlPath;
        var consoleConfigLoop = readConsoleConfigLoop(consoleConfigLoopPath);

        var loopSetups = consoleConfigLoop.getLoopSetups();

        var totalRunTime = loopSetups.stream().mapToInt(i -> i.getGraspSettings().timeLimit()).sum();
        System.out.println("Expected total run time: " + totalRunTime / 3600 + " hours");

        for (var loopSetUp : loopSetups) {
            var parameters = new JsonParser().readData(loopSetUp.getInstancePath());
            var outputDirPath = loopSetUp.getOutputDirPath(consoleConfigLoop.outputDirectory);
            var file = new File(outputDirPath + "/solution.json");
            if (file.exists()) {
                System.out.println("Solution already exists for " + loopSetUp.getInstancePath());
                continue;
            }

            var solverSolution = runHeuristic(parameters, loopSetUp.getGraspSettings());
            Utils.feasibilityCheck(solverSolution.getBestSolution());

            var path = Paths.get(outputDirPath);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }

            Utils.feasibilityCheck(solverSolution.getBestSolution());
            Utils.writeObjectToJson(solverSolution, file.getPath());
        }
    }

    private static SolverSolution runHeuristic(
            ProblemParameters parameters, GraspSettings graspSettings) throws Exception {
        return new GraspWithPathRelinking(parameters, graspSettings).getSolution();
    }

    private static ConsoleConfigLoop readConsoleConfigLoop(String consoleConfigLoopPath)
            throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(consoleConfigLoopPath);

        var config = yaml.loadAs(inputStream, ConsoleConfigLoop.class);

        if (config.instancePaths == null || config.instancePaths.isEmpty()) {
            var file = new File(config.instanceFolder);
            var instanceFiles = file.listFiles((dir, name) -> name.endsWith(".json"));
            assert instanceFiles != null;

            config.instancePaths = new ArrayList<>();
            for (var instanceFile : instanceFiles) {
                config.instancePaths.add(instanceFile.getPath());
            }
        }

        return config;
    }
}

class AlphaGeneratorWrapper {
    public String type;
    public double alpha;

    public double minAlpha;
    public double maxAlpha;

    public AlphaGenerator getAlphaGenerator() {
        if (type.equals("Fixed")) {
            return new AlphaGeneratorConstant(alpha);
        }
        return new AlphaGeneratorUniform(new Random(), minAlpha, maxAlpha);
    }
}

class ConsoleConfigLoop {
    public List<String> instancePaths;
    public String instanceFolder;
    public List<SearchMode> searchModes;
    public List<String> localSearchMoves;
    public List<AlphaGeneratorWrapper> alphaGeneratorOptions;
    public List<Double> localSearchRandomProbabilities;
    public int timeLimit;
    public int randomRunN;
    public String outputDirectory;

    public List<LoopSetup> getLoopSetups() {
        var loopSetups = new ArrayList<LoopSetup>();

        System.out.println(
                "Expected run time: "
                        + instancePaths.size()
                                * searchModes.size()
                                * alphaGeneratorOptions.size()
                                * localSearchRandomProbabilities.size()
                                * timeLimit
                                * randomRunN
                                / 60
                                / 60
                        + " hours");

        for (var instancePath : instancePaths) {
            for (var searchMode : searchModes) {
                for (var alphaGeneratorWrapper : alphaGeneratorOptions) {
                    for (var localSearchRandomProbability : localSearchRandomProbabilities) {
                        for (int randomRun = 0; randomRun < randomRunN; randomRun++) {
                            var constructiveHeuristicSettings =
                                    new ConstructiveHeuristicSettings(0.5, 2);
                            var localSearchSettings =
                                    new LocalSearchSettings(
                                            localSearchMoves, localSearchRandomProbability);

                            var graspSettings =
                                    new GraspSettings(
                                            searchMode,
                                            timeLimit,
                                            localSearchSettings,
                                            constructiveHeuristicSettings,
                                            new Random(),
                                            alphaGeneratorWrapper.getAlphaGenerator(),
                                            randomRun,
                                            instancePath);

                            var loopSetUp = new LoopSetup(graspSettings, randomRun, instancePath);

                            loopSetups.add(loopSetUp);
                        }
                    }
                }
            }
        }

        return loopSetups;
    }
}
