package scheduling.solver.heuristic.grasp;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;
import scheduling.solver.heuristic.grasp.vnd.selector.SequentialSelector;
import scheduling.solver.heuristic.grasp.vnd.statistics.PhaseStatistics;
import scheduling.solver.heuristic.grasp.vnd.statistics.SearchStatistics;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;

class GraspInformationTest {

    @Test
    void storesAndReturnsAllFields() {
        var vndConfig =
                new VNDConfig(
                        new FirstImprovingStrategy(), List.of(), new SequentialSelector(), 0.0);
        var config = new GraspConfig(new RunInfo("test", 0), 60, 5, vndConfig, 1.0, 1.0, 100);
        var searchStatistics = new SearchStatistics();
        var constructionStatistics = new PhaseStatistics();
        var localSearchStatistics = new PhaseStatistics();
        var pathRelinkingStatistics = new PhaseStatistics();

        var info =
                new GraspInformation(
                        config,
                        searchStatistics,
                        constructionStatistics,
                        localSearchStatistics,
                        pathRelinkingStatistics);

        assertSame(config, info.getConfig());
        assertSame(searchStatistics, info.getSearchStatistics());
        assertSame(constructionStatistics, info.getConstructionStatistics());
        assertSame(localSearchStatistics, info.getLocalSearchStatistics());
        assertSame(pathRelinkingStatistics, info.getPathRelinkingStatistics());
    }
}
