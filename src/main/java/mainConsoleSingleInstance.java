import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.Utils;
import grasp.graspWithPathRelinking.ReactiveGraspWithPathRelinking;
import grasp.reactiveGrasp.AlphaGenerator;
import grasp.reactiveGrasp.AlphaGeneratorConstant;
import grasp.reactiveGrasp.AlphaGeneratorUniform;
import model.ProblemParameters;
import runParameters.ConstructiveHeuristicSettings;
import runParameters.LocalSearchSettings;

import java.util.List;
import java.util.Random;

@Parameters(separators = "=")
public class mainConsoleSingleInstance {

    @Parameter(names = {"--instancePath"}, description = "Path to the instance file.", required = true)
    String instancePath;

    @Parameter(names = {"--isBestMove"}, description = "Local search improving move strategy.", required = true, arity = 1)
    boolean isBestMove = false;

    @Parameter(names = {"--alphaGeneratorType"}, description = "Defines the alpha generator type.", required = true)
    String alphaGeneratorType;

    @Parameter(names = {"--alphaGeneratorRange"}, description = "Defines the alpha generator range.", required = true)
    List<Double> alphaGeneratorRange;

    @Parameter(names = {"--timeLimit"}, description = "Time limit in seconds.", required = true)
    int timeLimit;

    @Parameter(names = {"--seed"}, description = "Seed for random number generator.")
    Long seed = null;

    @Parameter(names = {"--outputPath"}, description = "Path to the instance file.")
    String outputPath = "solution.json";

    public static void main(String... args) throws Exception {
        mainConsoleSingleInstance main = new mainConsoleSingleInstance();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .build();
//        commander.parse("--instancePath=instances/1.json", "--isBestMove=True", "--alphaGeneratorType=U", "--alphaGeneratorRange=0.1,0.5");
        commander.parse(args);
        commander.setProgramName("mainConsoleSingleInstance");

        // Validate alphaGeneratorType
        if (!List.of("uniform", "fixed", "U", "F").contains(main.alphaGeneratorType)) {
            throw new Exception("Invalid alpha generator type. Valid types are: uniform, fixed, U, F.");
        }

        // Validate alphaGeneratorRange
        if ((main.alphaGeneratorType.equals("uniform") || main.alphaGeneratorType.equals("U")) &&
                main.alphaGeneratorRange.size() != 2) {
            throw new Exception("Invalid alpha generator range for uniform distribution. Exactly two values required.");
        }

        if ((main.alphaGeneratorType.equals("fixed") || main.alphaGeneratorType.equals("F")) &&
                main.alphaGeneratorRange.size() != 1) {
            throw new Exception("Invalid alpha generator range for fixed distribution. Exactly one value required.");
        }

        var parameters = new ProblemParameters();
        parameters.readData(main.instancePath);

        var constructiveHeuristicSettings = new ConstructiveHeuristicSettings(
                0.5,
                5
        );
        var localSearchSettings = new LocalSearchSettings(
                List.of("shift", "intraSwap", "transfer", "interSwap", "insert", "outOfPool")
        );

        Random random;
        if (main.seed != null) random = new Random(main.seed);
        else random = new Random();

        AlphaGenerator alphaGenerator;
        if (main.alphaGeneratorType.equals("uniform") || main.alphaGeneratorType.equals("U")) {
            alphaGenerator = new AlphaGeneratorUniform(random, main.alphaGeneratorRange.get(0), main.alphaGeneratorRange.get(1));
        } else {
            alphaGenerator = new AlphaGeneratorConstant(main.alphaGeneratorRange.getFirst());
        }

        var graspSettings = new runParameters.GraspSettings(
                main.isBestMove,
                main.timeLimit,
                localSearchSettings,
                constructiveHeuristicSettings,
                random,
                alphaGenerator,
                0,
                main.instancePath
        );

        var grasp = new ReactiveGraspWithPathRelinking(parameters, graspSettings);

        var graspOutput = grasp.getGraspOutput();
        Utils.writeObjectToJson(graspOutput, main.outputPath);
    }
}
