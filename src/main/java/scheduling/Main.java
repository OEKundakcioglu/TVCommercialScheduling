package scheduling;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import scheduling.mapping.ProblemDTOReader;
import scheduling.model.Problem;
import scheduling.model.ProblemBuilder;
import scheduling.solver.RunInfo;
import scheduling.solver.heuristic.grasp.GraspConfig;
import scheduling.solver.heuristic.grasp.MultiThreadGraspAlgorithm;
import scheduling.solver.heuristic.grasp.vnd.VNDConfig;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.InsertNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.InterSwapNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.IntraSwapNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.Neighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.OutOfPoolSwapNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.ShiftNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.neighborhood.TransferNeighborhood;
import scheduling.solver.heuristic.grasp.vnd.selector.AdaptiveSelector;
import scheduling.solver.heuristic.grasp.vnd.strategy.FirstImprovingStrategy;

public class Main {
    public static void main(String[] args) {
        var problemPath = Path.of("json_files/1.json");

        var problemDTO = ProblemDTOReader.read(problemPath);
        var problem = ProblemBuilder.build(problemDTO);

        runSingleThreadGrasp(problem);
    }

    private static void runSingleThreadGrasp(Problem problem) {
        var random = new Random(42);
        var neighborhoods = buildNeighborhoods(problem);
        var vndConfig =
                new VNDConfig(
                        new FirstImprovingStrategy(),
                        neighborhoods,
                        new AdaptiveSelector(0.05, neighborhoods),
                        0.0);
        var config = new GraspConfig(new RunInfo("1", 42), 60, 10, vndConfig, 0.5, 2.0, 100);
        var algorithm = new MultiThreadGraspAlgorithm(config, random);

        var result = algorithm.run(problem);

        System.out.println("Best revenue: " + result.getBestSolution().getTotalRevenue());
        System.out.println("Checkpoints: " + result.getCheckPoints().size());
    }

    private static List<Neighborhood> buildNeighborhoods(Problem problem) {
        return List.of(
                new InsertNeighborhood(problem),
                new InterSwapNeighborhood(problem),
                new IntraSwapNeighborhood(problem),
                new OutOfPoolSwapNeighborhood(problem),
                new ShiftNeighborhood(problem),
                new TransferNeighborhood(problem));
    }
}
