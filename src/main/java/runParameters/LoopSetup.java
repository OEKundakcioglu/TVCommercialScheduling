package runParameters;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class LoopSetup {
    public static boolean isDebug = true;
    public static int numberOfCommercials = 400;
    private final String instancePath;
    private final ConstructiveHeuristicSettings constructiveHeuristicSettings;
    private final LocalSearchSettings localSearchSettings;
    private final GraspSettings graspSettings;
    private final int randomRunN;

    public LoopSetup(ConstructiveHeuristicSettings constructiveHeuristicSettings,
                     GraspSettings graspSettings,
                     int randomRunN,
                     String instancePath) {

        this.constructiveHeuristicSettings = constructiveHeuristicSettings;
        this.localSearchSettings = new LocalSearchSettings(
                List.of("shift", "intraSwap", "transfer", "interSwap", "insert", "outOfPool")
        );
        this.graspSettings = graspSettings;
        this.randomRunN = randomRunN;
        this.instancePath = instancePath;
    }

    public GraspSettings getGraspSettings() {
        return graspSettings;
    }

    public String getInstancePath() {
        return instancePath;
    }

    public String getOutputDirPath(String dirName){
        var instanceName = instancePath.split("/")[instancePath.split("/").length - 1];

        return String.format("%s/%s/%s",
                dirName,
                instanceName,
                graspSettings.getStringIdentifier());
    }
}
