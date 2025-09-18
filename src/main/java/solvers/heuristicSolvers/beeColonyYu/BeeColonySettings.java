package solvers.heuristicSolvers.beeColonyYu;

public record BeeColonySettings(
        int timeLimit,
        int populationSize,
        double alpha,
        int nIter,
        double T0,
        int n,
        String instancePath) {

    public String getStringIdentifier() {


        return String.format(
                "%d_%f_%d_%f_%d",
                populationSize, alpha, nIter, T0, n);
    }

    public String getOutputDirPath(String dirName){
        var instanceName = instancePath.split("/")[instancePath.split("/").length - 1].replace(".json", "");

        return String.format("%s/%s/%s",
                dirName,
                instanceName,
                getStringIdentifier());
    }
}
