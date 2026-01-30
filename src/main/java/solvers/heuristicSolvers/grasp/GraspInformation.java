package solvers.heuristicSolvers.grasp;

import runParameters.GraspSettings;
import solvers.heuristicSolvers.grasp.localSearch.MoveStatistics;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class GraspInformation {
    private final GraspSettings settings;
    private final double iterationsPerSecond;
    private final MoveStatistics moveStatistics;

    public GraspInformation(GraspSettings settings, double iterationsPerSecond, MoveStatistics moveStatistics) {
        this.settings = settings;
        this.iterationsPerSecond = iterationsPerSecond;
        this.moveStatistics = moveStatistics;
    }

    /**
     * Get the move statistics from the GRASP run.
     * Returns null if statistics tracking was not enabled.
     */
    public MoveStatistics getMoveStatistics() {
        return moveStatistics;
    }
}
