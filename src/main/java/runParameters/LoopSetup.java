package runParameters;


@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class LoopSetup {
    public static boolean isDebug = false;
    public static int numberOfCommercials = 400;
    private final String instancePath;
    private final ConstructiveHeuristicSettings constructiveHeuristicSettings;
    private final LocalSearchSettings localSearchSettings;
    private final GraspSettings graspSettings;
    private final int randomRunN;

    public LoopSetup(GraspSettings graspSettings, int randomRunN, String instancePath) {

        this.constructiveHeuristicSettings = graspSettings.constructiveHeuristicSettings();
        this.localSearchSettings = graspSettings.localSearchSettings();
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

    public String getOutputDirPath(String dirName) {
        var instanceName = instancePath.split("/")[instancePath.split("/").length - 1];
        instanceName = instanceName.replace(".json", "");

        return String.format(
                "%s/%s/%s", dirName, instanceName, graspSettings.getStringIdentifier());
    }
}
