package scheduling.solver.heuristic.grasp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.solver.Solution;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SolutionConverter {

    public static Solution toSolution(Problem problem, GraspSolution graspSolution) {
        var inventories = problem.getInventories();
        var sequences = graspSolution.getSequences();

        ImmutableMap.Builder<Inventory, List<Commercial>> assignmentsBuilder =
                ImmutableMap.builder();
        var totalRevenue = 0.0;

        for (int invId = 0; invId < inventories.length; invId++) {
            var inventory = inventories[invId];
            var sequence = sequences[invId];

            ImmutableList.Builder<Commercial> commercials = ImmutableList.builder();
            var startTime = 0;
            for (int commId : sequence) {
                var commercial = problem.getCommercial(commId);
                commercials.add(commercial);
                totalRevenue += problem.getRevenue(commId, invId, startTime);
                startTime += commercial.getDuration();
            }
            assignmentsBuilder.put(inventory, commercials.build());
        }

        return new Solution(assignmentsBuilder.build(), totalRevenue);
    }
}
