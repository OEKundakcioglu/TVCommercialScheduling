package scheduling.solver.heuristic.grasp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.solver.heuristic.grasp.vnd.statistics.PhaseStatistics;
import scheduling.solver.heuristic.grasp.vnd.statistics.SearchStatistics;

@Getter
@RequiredArgsConstructor
public class GraspInformation {

    private final GraspConfig config;
    private final SearchStatistics searchStatistics;
    private final PhaseStatistics constructionStatistics;
    private final PhaseStatistics localSearchStatistics;
    private final PhaseStatistics pathRelinkingStatistics;
}
