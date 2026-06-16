package scheduling.solver.heuristic.grasp.pathrelinking;

import java.util.Optional;
import java.util.Random;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.move.InsertMove;
import scheduling.solver.heuristic.grasp.move.Move;
import scheduling.solver.heuristic.grasp.move.RemoveMove;
import scheduling.solver.heuristic.grasp.move.TransferMove;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathRelinkingUtils {

    public static Optional<Move> selectMove(
            Problem problem, GraspSolution current, GraspSolution guiding, Random random) {
        var currentInv = current.getAssignedInvId();
        var currentPos = current.getAssignedPos();
        var guidingInv = guiding.getAssignedInvId();
        var numComm = currentInv.length;

        Move selected = null;
        var count = 0;

        for (int commId = 0; commId < numComm; commId++) {
            if (currentInv[commId] == guidingInv[commId]) {
                continue;
            }

            var isInCurrent = currentInv[commId] != -1;
            var isInGuiding = guidingInv[commId] != -1;

            if (isInCurrent && isInGuiding) {
                var fromInvId = currentInv[commId];
                var fromPos = currentPos[commId];
                var toInvId = guidingInv[commId];
                var toSeqLen = current.getSequences()[toInvId].length;
                for (int pos = 0; pos <= toSeqLen; pos++) {
                    var move = new TransferMove(problem, current, fromInvId, fromPos, toInvId, pos);
                    if (move.checkFeasibility()) {
                        count++;
                        if (random.nextInt(count) == 0) {
                            selected = move;
                        }
                    }
                }
            } else if (isInCurrent) {
                var move = new RemoveMove(problem, current, currentInv[commId], currentPos[commId]);
                if (move.checkFeasibility()) {
                    count++;
                    if (random.nextInt(count) == 0) {
                        selected = move;
                    }
                }
            } else {
                var toInvId = guidingInv[commId];
                var toSeqLen = current.getSequences()[toInvId].length;
                for (int pos = 0; pos <= toSeqLen; pos++) {
                    var move = new InsertMove(problem, current, toInvId, pos, commId);
                    if (move.checkFeasibility()) {
                        count++;
                        if (random.nextInt(count) == 0) {
                            selected = move;
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(selected);
    }

    public static int distance(GraspSolution a, GraspSolution b, int numCommercials) {
        var assignedInA = a.getAssignedInvId();
        var assignedInB = b.getAssignedInvId();
        var dist = 0;
        for (int commId = 0; commId < numCommercials; commId++) {
            if (assignedInA[commId] == assignedInB[commId]) {
                continue;
            }
            dist++;
        }
        return dist;
    }
}
