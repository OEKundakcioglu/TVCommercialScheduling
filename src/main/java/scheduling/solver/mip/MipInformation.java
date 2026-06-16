package scheduling.solver.mip;

public record MipInformation(
        MipConfig config,
        int statusCode,
        String status,
        double objectiveValue,
        double objectiveBound,
        double mipGap,
        double runtimeSeconds,
        double nodeCount,
        int solutionCount) {}
