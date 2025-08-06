import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.Utils;
import data.problemBuilders.JsonParser;
import org.yaml.snakeyaml.Yaml;
import runParameters.MipRunSettings;
import solvers.mipSolvers.BaseModel;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.continuousTimeModel.ContinuousTimeModel;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class MipConfig {
    public String instanceFolder;
    public List<String> instancePaths;
    public List<Integer> checkPointTimes;
    public String outputDirectory;
    public String modelType;
}

@Parameters(separators = "=")
public class mainConsoleMipLoop {
    @Parameter(
            names = {"--yamlConfigPath", "--ycp"},
            description = "Path to the yaml config file")
    private String yamlConfigPath = "src/main/resources/config.yaml";

    public static void main(String[] args) throws Exception {
        mainConsoleMipLoop main = new mainConsoleMipLoop();
        JCommander commander = JCommander.newBuilder().addObject(main).build();
        commander.parse(args);

        var mipConfig = readConsoleConfigMipLoop(main.yamlConfigPath);

        for (var instancePath : mipConfig.instancePaths) {
            try{
                var parameters = new JsonParser().readData(instancePath);

                var mipRunSettings = new MipRunSettings(mipConfig.checkPointTimes, "");

                var instanceName = instancePath.split("/")[instancePath.split("/").length - 1];

                var file = new File(String.format("%s/%s/solution.json", mipConfig.outputDirectory, instanceName));
                if (file.exists()) {
                    System.out.println("Solution already exists for " + instanceName);
                    continue;
                }

                BaseModel model;
                if (mipConfig.modelType.equals("Discrete")) model = new DiscreteTimeModel(parameters);
                else if (mipConfig.modelType.equals("Continuous"))
                    model = new ContinuousTimeModel(parameters);
                else throw new Exception("Invalid model type");

                var solver = new ModelSolver(model, parameters, mipRunSettings);
                var solverSolution = solver.getSolution();

                Utils.feasibilityCheck(solverSolution.getBestSolution());

                Utils.writeObjectToJson(
                        solverSolution,
                        file.getPath());

                System.gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static MipConfig readConsoleConfigMipLoop(String path) throws FileNotFoundException {
        var yaml = new Yaml();
        InputStream inputStream = new FileInputStream(path);

        var config = yaml.loadAs(inputStream, MipConfig.class);

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
