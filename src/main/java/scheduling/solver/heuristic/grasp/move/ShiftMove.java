package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class ShiftMove extends Move {

    private final int invId;
    private final int fromPos;
    private final int toPos;

    public ShiftMove(Problem problem, GraspSolution solution, int invId, int fromPos, int toPos) {
        super(problem, solution);
        this.invId = invId;
        this.fromPos = fromPos;
        this.toPos = toPos;
    }

    @Override
    public boolean checkFeasibility() {
        if (fromPos == toPos) {
            return true;
        }
        var seq = solution.getSequences()[invId];
        var seqLen = seq.length;
        var shiftedCommId = seq[fromPos];

        if (!checkAttentionFeasibility(seq, seqLen, shiftedCommId)) {
            return false;
        }
        return checkGroupFeasibility(seq, shiftedCommId);
    }

    private boolean checkAttentionFeasibility(int[] seq, int seqLen, int shiftedCommId) {
        if (!isAttentionSatisfied(shiftedCommId, invId, toPos, seqLen)) {
            return false;
        }
        int minAffected;
        int maxAffected;
        int posShift;
        if (fromPos < toPos) {
            minAffected = fromPos + 1;
            maxAffected = toPos;
            posShift = -1;
        } else {
            minAffected = toPos;
            maxAffected = fromPos - 1;
            posShift = 1;
        }
        for (int p = minAffected; p <= Math.min(2, maxAffected); p++) {
            if (!isAttentionSatisfied(seq[p], invId, p + posShift, seqLen)) {
                return false;
            }
        }
        for (int p = Math.max(seqLen - 3, minAffected); p <= maxAffected; p++) {
            if (!isAttentionSatisfied(seq[p], invId, p + posShift, seqLen)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkGroupFeasibility(int[] seq, int shiftedCommId) {
        if (!isGroupSatisfied(commAt(seq, fromPos - 1), commAt(seq, fromPos + 1))) {
            return false;
        }
        if (fromPos < toPos) {
            return isGroupSatisfied(seq[toPos], shiftedCommId, commAt(seq, toPos + 1));
        }
        return isGroupSatisfied(commAt(seq, toPos - 1), shiftedCommId, seq[toPos]);
    }

    @Override
    protected double computeRevenueGain() {
        var seq = solution.getSequences()[invId];
        var startTimes = solution.getStartTimes()[invId];
        var revenues = solution.getRevenues()[invId];
        var shiftedCommId = seq[fromPos];
        var shiftedCommDur = problem.getCommercial(shiftedCommId).getDuration();

        var delta = 0.0;

        if (fromPos < toPos) {
            delta += calculateRevenueChange(invId, fromPos + 1, toPos, -shiftedCommDur);
            var newStartTime =
                    startTimes[toPos]
                            + problem.getCommercial(seq[toPos]).getDuration()
                            - shiftedCommDur;
            delta += problem.getRevenue(shiftedCommId, invId, newStartTime) - revenues[fromPos];
        } else {
            delta += calculateRevenueChange(invId, toPos, fromPos - 1, shiftedCommDur);
            delta +=
                    problem.getRevenue(shiftedCommId, invId, startTimes[toPos]) - revenues[fromPos];
        }

        return delta;
    }

    @Override
    public GraspSolution apply() {
        return solution.toBuilder(problem)
                .replaceSequence(invId, buildNewSequence(), Math.min(fromPos, toPos))
                .addRevenue(calculateRevenueGain())
                .build();
    }

    private int[] buildNewSequence() {
        var seq = solution.getSequences()[invId];
        var newSeq = new int[seq.length];
        var shiftedCommId = seq[fromPos];

        if (fromPos < toPos) {
            System.arraycopy(seq, 0, newSeq, 0, fromPos);
            System.arraycopy(seq, fromPos + 1, newSeq, fromPos, toPos - fromPos);
            newSeq[toPos] = shiftedCommId;
            System.arraycopy(seq, toPos + 1, newSeq, toPos + 1, seq.length - toPos - 1);
        } else {
            System.arraycopy(seq, 0, newSeq, 0, toPos);
            newSeq[toPos] = shiftedCommId;
            System.arraycopy(seq, toPos, newSeq, toPos + 1, fromPos - toPos);
            System.arraycopy(seq, fromPos + 1, newSeq, fromPos + 1, seq.length - fromPos - 1);
        }

        return newSeq;
    }
}
