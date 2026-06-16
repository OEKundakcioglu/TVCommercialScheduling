package scheduling.solver.heuristic.grasp.vnd.neighborhood;

import java.util.BitSet;
import lombok.experimental.UtilityClass;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

@UtilityClass
class Neighborhoods {

    int[] findUnassignedCommercials(Problem problem, GraspSolution solution) {
        var assigned = new BitSet(problem.getCommercials().length);
        for (var sequence : solution.getSequences()) {
            for (var commId : sequence) {
                assigned.set(commId);
            }
        }
        var count = problem.getCommercials().length - assigned.cardinality();
        var unassigned = new int[count];
        var idx = 0;
        for (int c = 0; c < problem.getCommercials().length; c++) {
            if (!assigned.get(c)) {
                unassigned[idx++] = c;
            }
        }
        return unassigned;
    }
}
