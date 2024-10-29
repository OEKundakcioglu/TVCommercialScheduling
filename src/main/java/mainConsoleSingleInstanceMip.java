import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.Utils;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.continuousTimeModel.ContinuousTimeModel;
import data.ProblemParameters;
import runParameters.MipRunSettings;

import java.util.List;

@SuppressWarnings("unused")
@Parameters(separators = "=")
public class mainConsoleSingleInstanceMip {
    @Parameter(names = {"--instancePath"}, description = "Path to the instance file.", required = true)
    private String instancePath;

    @Parameter(names = {"--timeLimit"}, description = "Time limit for the MIP.", required = true)
    private int timeLimit;

    @Parameter(names = {"--outputPath"}, description = "Path to the output file.")
    private String outputPath = "solution.json";

    @Parameter(names = {"--checkPointTimes"}, description = "Check point times.")
    private List<Integer> checkPointTimes = List.of();

    public static void main(String... args) throws Exception {
        mainConsoleSingleInstanceMip main = new mainConsoleSingleInstanceMip();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .build();
        commander.parse(args);
        commander.setProgramName("mainConsoleSingleInstanceMip");

        var parameters = new ProblemParameters();
        parameters.readData(main.instancePath);

        var mipRunSettings = new MipRunSettings(
                main.checkPointTimes,
                main.outputPath
        );

        var solver = new ModelSolver(
                new ContinuousTimeModel(parameters),
                parameters,
                mipRunSettings
        );
        var solution = solver.getSolution();
        Utils.writeObjectToJson(solution, main.outputPath);
    }
}
