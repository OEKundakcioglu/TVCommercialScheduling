package scheduling.solver.heuristic.grasp.pathrelinking;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Random;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class MixedPathRelinking {

    private final Problem problem;
    private final GraspSolution initialSolution;
    private final GraspSolution guidingSolution;
    private final Random random;

    private GraspSolution current;
    private GraspSolution guiding;
    private boolean lastMoveFromCurrentNull;
    private boolean lastMoveFromGuidingNull;
    private boolean isDirectionTowardsGuiding;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Random is intentionally shared")
    public MixedPathRelinking(
            Problem problem,
            GraspSolution initialSolution,
            GraspSolution guidingSolution,
            Random random) {
        this.problem = problem;
        this.initialSolution = initialSolution;
        this.guidingSolution = guidingSolution;
        this.random = random;
        this.current = initialSolution;
        this.guiding = guidingSolution;
    }

    public GraspSolution relink() {
        current = initialSolution;
        guiding = guidingSolution;
        lastMoveFromCurrentNull = false;
        lastMoveFromGuidingNull = false;
        isDirectionTowardsGuiding = true;

        var bestSolution = initialSolution;
        var dist = PathRelinkingUtils.distance(current, guiding, problem.getCommercials().length);
        while (dist > 1) {
            var target = guiding;
            var moveOpt = PathRelinkingUtils.selectMove(problem, current, target, random);

            if (moveOpt.isEmpty()) {
                swapDirection(true);
                if (lastMoveFromCurrentNull && lastMoveFromGuidingNull) {
                    break;
                }
                continue;
            }

            current = moveOpt.get().apply();
            dist--;
            if (current.getTotalRevenue() > bestSolution.getTotalRevenue()) {
                bestSolution = current;
            }

            swapDirection(false);
        }

        return bestSolution;
    }

    private void swapDirection(boolean moveWasNull) {
        var temp = current;
        current = guiding;
        guiding = temp;
        if (isDirectionTowardsGuiding) {
            lastMoveFromCurrentNull = moveWasNull;
        } else {
            lastMoveFromGuidingNull = moveWasNull;
        }
        isDirectionTowardsGuiding = !isDirectionTowardsGuiding;
    }
}
