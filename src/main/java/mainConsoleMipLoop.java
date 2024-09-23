import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import data.Utils;
import model.Model;
import model.ProblemParameters;
import org.yaml.snakeyaml.Yaml;
import runParameters.MipRunSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

class MipConfig {
    public List<String> instancePaths;
    public int timeLimit;
    public String outputDirectory;
}

@Parameters(separators = "=")
public class mainConsoleMipLoop {
    @Parameter(names = {"--yamlConfigPath", "--ycp"}, description = "Path to the yaml config file")
    private String yamlConfigPath = "src/main/resources/config.yaml";

    public static void main(String[] args) throws Exception {
        mainConsoleMipLoop main = new mainConsoleMipLoop();
        JCommander commander = JCommander.newBuilder()
                .addObject(main)
                .build();
        commander.parse(args);

        var consoleConfigMipLoopPath = main.yamlConfigPath;
        var mipConfig = readConsoleConfigMipLoop(consoleConfigMipLoopPath);

        for (var instancePath : mipConfig.instancePaths) {
            var parameters = new ProblemParameters();
            parameters.readData(instancePath);

            var mipRunSettings = new MipRunSettings(
                    mipConfig.timeLimit,
                    ""
            );

            var instanceName = instancePath.split("/")[instancePath.split("/").length - 1];

            var mip = new Model(parameters, mipRunSettings);
            var solution = mip.solution;
            Utils.writeObjectToJson(solution, String.format("%s/%s/solution.json", mipConfig.outputDirectory, instanceName));
        }
    }

    private static MipConfig readConsoleConfigMipLoop(String path) throws FileNotFoundException {
        var yaml = new Yaml();
        InputStream inputStream = new FileInputStream(path);

        return yaml.loadAs(inputStream, MipConfig.class);
    }
}
