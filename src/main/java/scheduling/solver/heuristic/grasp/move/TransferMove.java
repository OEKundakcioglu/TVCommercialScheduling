package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class TransferMove extends Move {

    private final int fromInvId;
    private final int fromPos;
    private final int toInvId;
    private final int toPos;

    public TransferMove(
            Problem problem,
            GraspSolution solution,
            int fromInvId,
            int fromPos,
            int toInvId,
            int toPos) {
        super(problem, solution);
        this.fromInvId = fromInvId;
        this.fromPos = fromPos;
        this.toInvId = toInvId;
        this.toPos = toPos;
    }

    @Override
    public boolean checkFeasibility() {
        var fromSeq = solution.getSequences()[fromInvId];
        var commId = fromSeq[fromPos];
        var commDuration = problem.getCommercial(commId).getDuration();

        if (!problem.isSuitable(commId, toInvId)) {
            return false;
        }
        if (!isCommercialCountSatisfied(toInvId)) {
            return false;
        }
        if (!isDurationSatisfied(toInvId, commDuration)) {
            return false;
        }
        if (!checkHourlyLimitFeasibility(commDuration)) {
            return false;
        }
        var toSeq = solution.getSequences()[toInvId];
        if (!checkGroupFeasibility(fromSeq, toSeq, commId)) {
            return false;
        }
        if (!checkSourceAttentionFeasibility(fromSeq)) {
            return false;
        }
        return checkDestAttentionFeasibility(toSeq, commId);
    }

    private boolean checkHourlyLimitFeasibility(int commDuration) {
        var fromHour = problem.getInventory(fromInvId).getHour();
        var toHour = problem.getInventory(toInvId).getHour();
        if (fromHour == toHour) {
            return true;
        }
        var toHourDuration = solution.getTotalDurationOfHour()[toHour];
        return isHourlyLimitSatisfied(toHourDuration, commDuration);
    }

    private boolean checkGroupFeasibility(int[] fromSeq, int[] toSeq, int commId) {
        if (!isGroupSatisfied(commAt(fromSeq, fromPos - 1), commAt(fromSeq, fromPos + 1))) {
            return false;
        }
        return isGroupSatisfied(commAt(toSeq, toPos - 1), commId, commAt(toSeq, toPos));
    }

    private boolean checkSourceAttentionFeasibility(int[] fromSeq) {
        var fromLen = fromSeq.length;
        var newLen = fromLen - 1;

        // Shifted commercials: F-type (F2, F3) may break when shifting left
        if (fromPos <= 1) {
            for (int oldPos = fromPos + 1; oldPos <= Math.min(2, fromLen - 1); oldPos++) {
                if (!isAttentionSatisfied(fromSeq[oldPos], fromInvId, oldPos - 1, newLen)) {
                    return false;
                }
            }
        }

        // Non-shifted commercials: L-type (L2) may break when sequence shortens
        if (fromPos >= fromLen - 3) {
            for (int pos = Math.max(0, fromLen - 4); pos <= fromPos - 1; pos++) {
                if (!isAttentionSatisfied(fromSeq[pos], fromInvId, pos, newLen)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkDestAttentionFeasibility(int[] toSeq, int commId) {
        var toLen = toSeq.length;
        var newLength = toLen + 1;

        if (!isAttentionSatisfied(commId, toInvId, toPos, newLength)) {
            return false;
        }
        if (toPos <= 2) {
            for (int pos = toPos; pos <= Math.min(2, toLen - 1); pos++) {
                if (!isAttentionSatisfied(toSeq[pos], toInvId, pos + 1, newLength)) {
                    return false;
                }
            }
        }
        if (toPos >= toLen - 2) {
            for (int pos = Math.max(toLen - 3, 0); pos <= toPos - 1; pos++) {
                if (!isAttentionSatisfied(toSeq[pos], toInvId, pos, newLength)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected double computeRevenueGain() {
        var fromSeq = solution.getSequences()[fromInvId];
        var toSeq = solution.getSequences()[toInvId];
        var fromRevenues = solution.getRevenues()[fromInvId];
        var commId = fromSeq[fromPos];
        var commDuration = problem.getCommercial(commId).getDuration();

        var newStartTime = computeDestStartTime(toSeq);

        var delta = 0.0;
        delta += problem.getRevenue(commId, toInvId, newStartTime) - fromRevenues[fromPos];
        delta += calculateRevenueChange(fromInvId, fromPos + 1, -commDuration);
        delta += calculateRevenueChange(toInvId, toPos, commDuration);
        return delta;
    }

    private int computeDestStartTime(int[] toSeq) {
        if (toPos == 0) {
            return 0;
        }
        var toStartTimes = solution.getStartTimes()[toInvId];
        return toStartTimes[toPos - 1] + problem.getCommercial(toSeq[toPos - 1]).getDuration();
    }

    @Override
    public GraspSolution apply() {
        var fromSeq = solution.getSequences()[fromInvId];
        var toSeq = solution.getSequences()[toInvId];
        var commId = fromSeq[fromPos];
        var commDuration = problem.getCommercial(commId).getDuration();
        return solution.toBuilder(problem)
                .replaceSequence(fromInvId, buildRemovedSequence(fromSeq), fromPos)
                .replaceSequence(toInvId, buildInsertedSequence(toSeq, commId), toPos)
                .addDuration(fromInvId, -commDuration)
                .addDuration(toInvId, commDuration)
                .addRevenue(calculateRevenueGain())
                .build();
    }

    private int[] buildRemovedSequence(int[] fromSeq) {
        var newSeq = new int[fromSeq.length - 1];
        System.arraycopy(fromSeq, 0, newSeq, 0, fromPos);
        System.arraycopy(fromSeq, fromPos + 1, newSeq, fromPos, fromSeq.length - fromPos - 1);
        return newSeq;
    }

    private int[] buildInsertedSequence(int[] toSeq, int commId) {
        var newSeq = new int[toSeq.length + 1];
        System.arraycopy(toSeq, 0, newSeq, 0, toPos);
        newSeq[toPos] = commId;
        System.arraycopy(toSeq, toPos, newSeq, toPos + 1, toSeq.length - toPos);
        return newSeq;
    }
}
