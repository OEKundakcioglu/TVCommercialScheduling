package runParameters;

import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristicType;

/**
 * Settings for constructive heuristics used in GRASP.
 *
 * @param deviation                 Deviation factor for attention boost randomization.
 *                                  Attention boosts will be random values in range [1/k, k] where k is the deviation.
 *                                  Example: deviation=2 → range [0.5, 2], deviation=3 → range [0.333, 3]
 * @param constructiveHeuristicType Type of constructive heuristic (STANDARD or REGRET_BASED)
 * @param kRegret                   k value for k-regret calculation (only used when type is REGRET_BASED)
 */
public record ConstructiveHeuristicSettings(
        double deviation,
        ConstructiveHeuristicType constructiveHeuristicType,
        int kRegret
) {

    /**
     * Constructor with default values for backward compatibility.
     * Uses STANDARD heuristic type and kRegret=3.
     */
    public ConstructiveHeuristicSettings(double deviation) {
        this(deviation, ConstructiveHeuristicType.STANDARD, 3);
    }

    @SuppressWarnings("unused")
    public String getStringIdentifier() {
        return String.format(
                "deviation=%.2f_type=%s_kRegret=%d",
                this.deviation,
                this.constructiveHeuristicType.name(),
                this.kRegret
        );
    }
}
