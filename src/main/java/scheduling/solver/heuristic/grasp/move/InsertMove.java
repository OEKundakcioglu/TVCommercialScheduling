package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class InsertMove extends Move {

    private final int invId;
    private final int position;
    private final int commId;

    public InsertMove(
            Problem problem, GraspSolution solution, int invId, int position, int commId) {
        super(problem, solution);
        this.invId = invId;
        this.position = position;
        this.commId = commId;
    }

    @Override
    public boolean checkFeasibility() {
        if (!problem.isSuitable(commId, invId)) {
            return false;
        }

        var sequence = solution.getSequences()[invId];
        var n = sequence.length;
        var newLength = n + 1;
        var commDuration = problem.getCommercial(commId).getDuration();

        if (!checkAttentionFeasibility(sequence, n, newLength)) {
            return false;
        }
        if (!checkGroupFeasibility(sequence, n)) {
            return false;
        }
        if (!isCommercialCountSatisfied(invId)) {
            return false;
        }
        if (!isDurationSatisfied(invId, commDuration)) {
            return false;
        }
        return checkHourlyLimitFeasibility(commDuration);
    }

    @Override
    protected double computeRevenueGain() {
        var sequence = solution.getSequences()[invId];
        var startTimes = solution.getStartTimes()[invId];
        var commDuration = problem.getCommercial(commId).getDuration();

        var insertStartTime = 0;
        if (position > 0) {
            insertStartTime =
                    startTimes[position - 1]
                            + problem.getCommercial(sequence[position - 1]).getDuration();
        }

        var insertRevenue = problem.getRevenue(commId, invId, insertStartTime);
        var shiftDelta = calculateRevenueChange(invId, position, commDuration);
        return insertRevenue + shiftDelta;
    }

    @Override
    public GraspSolution apply() {
        var newSeq = buildNewSequence(solution.getSequences()[invId]);
        var commDuration = problem.getCommercial(commId).getDuration();
        return solution.toBuilder(problem)
                .replaceSequence(invId, newSeq, position)
                .addDuration(invId, commDuration)
                .addRevenue(calculateRevenueGain())
                .build();
    }

    private boolean checkAttentionFeasibility(int[] sequence, int n, int newLength) {
        if (!isAttentionSatisfied(commId, invId, position, newLength)) {
            return false;
        }
        if (position <= 2) {
            for (int pos = position; pos <= Math.min(2, n - 1); pos++) {
                if (!isAttentionSatisfied(sequence[pos], invId, pos + 1, newLength)) {
                    return false;
                }
            }
        }
        if (position >= n - 2) {
            for (int pos = Math.max(n - 3, 0); pos <= position - 1; pos++) {
                if (!isAttentionSatisfied(sequence[pos], invId, pos, newLength)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkGroupFeasibility(int[] sequence, int n) {
        var leftNeighbor = position > 0 ? sequence[position - 1] : -1;
        var rightNeighbor = position < n ? sequence[position] : -1;
        return isGroupSatisfied(leftNeighbor, commId, rightNeighbor);
    }

    private boolean checkHourlyLimitFeasibility(int commDuration) {
        var hour = problem.getInventory(invId).getHour();
        var currentHourDuration = solution.getTotalDurationOfHour()[hour];
        return isHourlyLimitSatisfied(currentHourDuration, commDuration);
    }

    private int[] buildNewSequence(int[] oldSequence) {
        var newSequence = new int[oldSequence.length + 1];
        System.arraycopy(oldSequence, 0, newSequence, 0, position);
        newSequence[position] = commId;
        System.arraycopy(
                oldSequence, position, newSequence, position + 1, oldSequence.length - position);
        return newSequence;
    }
}
