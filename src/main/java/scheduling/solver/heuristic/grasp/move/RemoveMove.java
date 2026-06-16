package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class RemoveMove extends Move {

    private final int invId;
    private final int position;

    public RemoveMove(Problem problem, GraspSolution solution, int invId, int position) {
        super(problem, solution);
        this.invId = invId;
        this.position = position;
    }

    @Override
    public boolean checkFeasibility() {
        var sequence = solution.getSequences()[invId];
        var n = sequence.length;
        var newLength = n - 1;

        if (!checkGroupFeasibility(sequence)) {
            return false;
        }
        return checkAttentionFeasibility(sequence, n, newLength);
    }

    @Override
    protected double computeRevenueGain() {
        var sequence = solution.getSequences()[invId];
        var removedCommDuration = problem.getCommercial(sequence[position]).getDuration();
        var lostRevenue = solution.getRevenues()[invId][position];
        var shiftDelta = calculateRevenueChange(invId, position + 1, -removedCommDuration);
        return -lostRevenue + shiftDelta;
    }

    @Override
    public GraspSolution apply() {
        var oldSeq = solution.getSequences()[invId];
        var commDuration = problem.getCommercial(oldSeq[position]).getDuration();
        var newSeq = buildNewSequence(oldSeq);
        return solution.toBuilder(problem)
                .replaceSequence(invId, newSeq, position)
                .addDuration(invId, -commDuration)
                .addRevenue(calculateRevenueGain())
                .build();
    }

    private boolean checkGroupFeasibility(int[] sequence) {
        var leftNeighbor = commAt(sequence, position - 1);
        var rightNeighbor = commAt(sequence, position + 1);
        return isGroupSatisfied(leftNeighbor, rightNeighbor);
    }

    private boolean checkAttentionFeasibility(int[] sequence, int n, int newLength) {
        if (position <= 2) {
            for (int pos = position + 1; pos <= Math.min(2, n - 1); pos++) {
                if (!isAttentionSatisfied(sequence[pos], invId, pos - 1, newLength)) {
                    return false;
                }
            }
        }
        if (position >= n - 3) {
            for (int pos = Math.max(0, n - 4); pos <= position - 1; pos++) {
                if (!isAttentionSatisfied(sequence[pos], invId, pos, newLength)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[] buildNewSequence(int[] oldSequence) {
        var newSequence = new int[oldSequence.length - 1];
        System.arraycopy(oldSequence, 0, newSequence, 0, position);
        System.arraycopy(
                oldSequence,
                position + 1,
                newSequence,
                position,
                oldSequence.length - position - 1);
        return newSequence;
    }
}
