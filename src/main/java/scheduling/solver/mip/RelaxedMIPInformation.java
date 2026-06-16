package scheduling.solver.mip;

public record RelaxedMIPInformation(
        RelaxedMIPConfig config,
        int statusCode,
        String status,
        double relaxedIncumbentValue,
        double relaxedUpperBound,
        double realizedRevenue,
        double mipGap,
        double runtimeSeconds,
        double nodeCount,
        int solutionCount) {}
