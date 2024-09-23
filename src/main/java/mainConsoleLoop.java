import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.Utils;
import grasp.GraspOutput;
import grasp.graspWithPathRelinking.ReactiveGraspWithPathRelinking;
import grasp.reactiveGrasp.AlphaGenerator;
import grasp.reactiveGrasp.AlphaGeneratorConstant;
import grasp.reactiveGrasp.AlphaGeneratorUniform;
import model.ProblemParameters;
import org.yaml.snakeyaml.Yaml;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.GraspSettings;
import runParameters.LocalSearchSettings;
import runParameters.LoopSetup;

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
    @Parameter(names = {"--yamlConfigPath", "--ycp"}, description = "Path to the yaml configuration file.", required = true)
    private String yamlPath;


    public static void main(String[] args) throws Exception {
        mainConsoleLoop main = new mainConsoleLoop();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .build();
        commander.parse(args);

        var consoleConfigLoopPath = main.yamlPath;
        var consoleConfigLoop = readConsoleConfigLoop(consoleConfigLoopPath);

        var loopSetups = consoleConfigLoop.getLoopSetups();

        for (var loopSetUp : loopSetups) {
            var parameters = new ProblemParameters();
            parameters.readData(loopSetUp.getInstancePath());
            var graspOutput = runHeuristic(parameters, loopSetUp.getGraspSettings());

            var outputDirPath = loopSetUp.getOutputDirPath(consoleConfigLoop.outputDirectory);
            var path = Paths.get(outputDirPath);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }

            Utils.feasibilityCheck(graspOutput.getBestSolution());
            Utils.writeObjectToJson(graspOutput, outputDirPath + "/solution.json");
        }
    }

    private static GraspOutput runHeuristic(ProblemParameters parameters, GraspSettings graspSettings) throws Exception {
        return new ReactiveGraspWithPathRelinking(parameters, graspSettings).getGraspOutput();
    }

    private static ConsoleConfigLoop readConsoleConfigLoop(String consoleConfigLoopPath) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(consoleConfigLoopPath);

        return yaml.loadAs(inputStream, ConsoleConfigLoop.class);
    }
}


class AlphaGeneratorWrapper{
    public String type;
    public double alpha;

    public double minAlpha;
    public double maxAlpha;

    public AlphaGenerator getAlphaGenerator(){
        if(type.equals("Fixed")){
            return new AlphaGeneratorConstant(alpha);
        }
        return new AlphaGeneratorUniform(new Random(), minAlpha, maxAlpha);
    }
}

class ConsoleConfigLoop {
    public List<String> instancePaths;
    public List<Boolean> isBestMoveOptions;
    public List<AlphaGeneratorWrapper> alphaGeneratorOptions;
    public int timeLimit;
    public int randomRunN;
    public String outputDirectory;


    public List<LoopSetup> getLoopSetups() {
        var loopSetups = new ArrayList<LoopSetup>();

        for (var instancePath : instancePaths) {
            for (var isBestMove : isBestMoveOptions) {
                for (var alphaGeneratorWrapper : alphaGeneratorOptions) {
                    for (int randomRun = 0; randomRun < randomRunN; randomRun++) {
                        var constructiveHeuristicSettings = new ConstructiveHeuristicSettings(
                                0.5,
                                5
                        );
                        var localSearchSettings = new LocalSearchSettings(
                                List.of("shift", "intraSwap", "transfer", "interSwap", "insert", "outOfPool")
                        );

                        var graspSettings = new GraspSettings(
                                isBestMove,
                                timeLimit,
                                localSearchSettings,
                                constructiveHeuristicSettings,
                                new Random(),
                                alphaGeneratorWrapper.getAlphaGenerator(),
                                randomRun,
                                instancePath
                        );

                        var loopSetUp = new LoopSetup(
                                constructiveHeuristicSettings,
                                graspSettings,
                                randomRun,
                                instancePath
                        );

                        loopSetups.add(loopSetUp);
                    }
                }
            }
        }

        return loopSetups;
    }
}

