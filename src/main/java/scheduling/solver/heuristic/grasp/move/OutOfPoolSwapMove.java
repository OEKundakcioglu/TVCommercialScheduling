package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class OutOfPoolSwapMove extends Move {

    private final int invId;
    private final int position;
    private final int newCommId;

    public OutOfPoolSwapMove(
            Problem problem, GraspSolution solution, int invId, int position, int newCommId) {
        super(problem, solution);
        this.invId = invId;
        this.position = position;
        this.newCommId = newCommId;
    }

    @Override
    public boolean checkFeasibility() {
        var sequence = solution.getSequences()[invId];
        var oldCommId = sequence[position];
        var durationDelta =
                problem.getCommercial(newCommId).getDuration()
                        - problem.getCommercial(oldCommId).getDuration();

        if (!problem.isSuitable(newCommId, invId)) {
            return false;
        }
        if (durationDelta > 0) {
            if (!isDurationSatisfied(invId, durationDelta)) {
                return false;
            }
            var hourDuration =
                    solution.getTotalDurationOfHour()[problem.getInventory(invId).getHour()];
            if (!isHourlyLimitSatisfied(hourDuration, durationDelta)) {
                return false;
            }
        }
        if (!isAttentionSatisfied(newCommId, invId, position, sequence.length)) {
            return false;
        }
        return isGroupSatisfied(
                commAt(sequence, position - 1), newCommId, commAt(sequence, position + 1));
    }

    @Override
    protected double computeRevenueGain() {
        var sequence = solution.getSequences()[invId];
        var startTimes = solution.getStartTimes()[invId];
        var revenues = solution.getRevenues()[invId];
        var oldCommId = sequence[position];
        var durationDelta =
                problem.getCommercial(newCommId).getDuration()
                        - problem.getCommercial(oldCommId).getDuration();

        var delta = 0.0;
        delta += problem.getRevenue(newCommId, invId, startTimes[position]) - revenues[position];
        if (durationDelta != 0) {
            delta += calculateRevenueChange(invId, position + 1, durationDelta);
        }
        return delta;
    }

    @Override
    public GraspSolution apply() {
        var seq = solution.getSequences()[invId];
        var durationDelta =
                problem.getCommercial(newCommId).getDuration()
                        - problem.getCommercial(seq[position]).getDuration();
        var newSeq = seq.clone();
        newSeq[position] = newCommId;
        return solution.toBuilder(problem)
                .replaceSequence(invId, newSeq, position)
                .addDuration(invId, durationDelta)
                .addRevenue(calculateRevenueGain())
                .build();
    }
}
