package solvers.heuristicSolvers.beeColonyYu;

public record BeeColonySettings(
        int timeLimit,
        int populationSize,
        double alpha,
        int nIter,
        double T0,
        int n,
        String instancePath) {

    public int getSeed() {
        return String.format(
                        "%d_%f_%d_%f_%d_%s",
                        populationSize, alpha, nIter, T0, n, instancePath)
                .hashCode();
    }

    private String getStringIdentifier() {
        return String.format(
                "%d_%d_%f_%d_%f_%d",
                timeLimit, populationSize, alpha, nIter, T0, n);
    }

    public String getOutputDirPath(String dirName){
        var instanceName = instancePath.split("/")[instancePath.split("/").length - 1];

        return String.format("%s/%s/%s",
                dirName,
                instanceName,
                getStringIdentifier());
    }
}
