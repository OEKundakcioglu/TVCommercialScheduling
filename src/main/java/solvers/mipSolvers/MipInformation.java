package solvers.mipSolvers;

public record MipInformation(
        int timeLimit,
        double runTime,
        double objectiveValue,
        double gap,
        double upperBound
) {
}
