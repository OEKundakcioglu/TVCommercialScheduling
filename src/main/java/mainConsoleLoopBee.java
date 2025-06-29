import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import data.ProblemParameters;
import data.Utils;
import data.problemBuilders.JsonParser;

import org.yaml.snakeyaml.Yaml;

import solvers.heuristicSolvers.beeColonyYu.BeeColonyAlgorithm;
import solvers.heuristicSolvers.beeColonyYu.BeeColonySettings;
import solvers.heuristicSolvers.beeColonyYu.OrienteeringData;
import solvers.heuristicSolvers.beeColonyYu.ReduceProblemToVRP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Parameters(separators = "=")
public class mainConsoleLoopBee {
    @Parameter(
            names = {"--yamlConfigPath", "--ycp"},
            description = "Path to the yaml configuration file.",
            required = true)
    private String yamlPath;

    public static void main(String[] args) throws Exception {
        mainConsoleLoopBee main = new mainConsoleLoopBee();
        JCommander commander = JCommander.newBuilder().addObject(main).build();
        commander.parse(args);

        var consoleConfigLoopPath = main.yamlPath;
        var consoleConfigLoop = readConsoleConfigLoop(consoleConfigLoopPath);

        var loopSetups = consoleConfigLoop.getLoopSetups();

        var totalRunTime = loopSetups.stream()
                .mapToInt(BeeColonySettings::timeLimit)
                .sum();
        System.out.println("Expected total run time: " + totalRunTime / 3600 + " hours");

        var orienteeringMap = new HashMap<String, OrienteeringData>();
        var problemDataMap = new HashMap<String, ProblemParameters>();

        for (var loopSetUp : loopSetups) {
            OrienteeringData orienteeringData = null;
            ProblemParameters parameters = null;
            if (!orienteeringMap.containsKey(loopSetUp.instancePath())) {
                parameters = new JsonParser().readData(loopSetUp.instancePath());

                orienteeringData = ReduceProblemToVRP.reduce(parameters);

                orienteeringMap.put(loopSetUp.instancePath(), orienteeringData);
                problemDataMap.put(loopSetUp.instancePath(), parameters);
            } else {
                orienteeringData = orienteeringMap.get(loopSetUp.instancePath());
                parameters = problemDataMap.get(loopSetUp.instancePath());
            }

            var outputDirPath = loopSetUp.getOutputDirPath(consoleConfigLoop.outputDirectory);
            var outputFile = new File(outputDirPath + "/solution.json");
            if (outputFile.exists()) {
                System.out.println("Solution already exists for " + loopSetUp.instancePath());
                continue;
            }

            var beeColonyAlgorithm =
                    new BeeColonyAlgorithm(orienteeringData, loopSetUp, parameters);

            var solverSolution = beeColonyAlgorithm.getSolverSolution();

            Utils.feasibilityCheck(solverSolution.getBestSolution());

            var path = Paths.get(outputDirPath);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }

            Utils.feasibilityCheck(solverSolution.getBestSolution());
            Utils.writeObjectToJson(solverSolution, outputFile.getPath());
        }
    }

    private static BeeConsoleConfig readConsoleConfigLoop(String consoleConfigLoopPath)
            throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(consoleConfigLoopPath);

        var config = yaml.loadAs(inputStream, BeeConsoleConfig.class);

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

class BeeConsoleConfig {
    public String instanceFolder;
    public List<String> instancePaths;
    public List<Double> T0Options;
    public List<Double> alphaOptions;
    public List<Integer> populationSizeOptions;
    public int timeLimit;
    public int randomRunN;
    public String outputDirectory;

    public List<BeeColonySettings> getLoopSetups() {
        var loopSetups = new ArrayList<BeeColonySettings>();

        for (var instancePath : instancePaths) {
            for (var T0 : T0Options) {
                for (var alpha : alphaOptions) {
                    for (var populationSize : populationSizeOptions) {
                        for (var n = 0; n < randomRunN; n++) {
                            var settings =
                                    new BeeColonySettings(
                                            timeLimit,
                                            populationSize,
                                            alpha,
                                            10,
                                            T0,
                                            n,
                                            instancePath);
                            loopSetups.add(settings);
                        }
                    }
                }
            }
        }

        return loopSetups;
    }
}
