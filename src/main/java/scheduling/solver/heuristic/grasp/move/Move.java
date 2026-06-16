package scheduling.solver.heuristic.grasp.move;

import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.solver.heuristic.grasp.GraspSolution;

// Abstract base class for all local search moves in the GRASP heuristic.
//
// Each concrete move takes Problem and GraspSolution in its constructor (plus
// move-specific parameters like inventory IDs, positions, commercial IDs).
//
// Three abstract methods define the move contract:
// - checkFeasibility(): returns true if the move respects all constraints,
//   reasoning directly from current state + move parameters (no copy, no apply).
// - computeRevenueGain(): returns the delta in total revenue if this move were
//   applied, computed efficiently without modifying the solution.
// - apply(): returns a NEW GraspSolution with the move applied, leaving the
//   original unmodified.
//
// Protected utility methods provide reusable constraint checks and revenue
// calculation so that concrete moves stay short and readable.
@RequiredArgsConstructor
public abstract class Move {

    protected final Problem problem;
    protected final GraspSolution solution;

    private boolean revenueGainComputed;
    private double cachedRevenueGain;

    public abstract boolean checkFeasibility();

    protected abstract double computeRevenueGain();

    public final double calculateRevenueGain() {
        if (!revenueGainComputed) {
            cachedRevenueGain = computeRevenueGain();
            revenueGainComputed = true;
        }
        return cachedRevenueGain;
    }

    public abstract GraspSolution apply();

    protected boolean isAttentionSatisfied(
            int commId, int invId, int position, int sequenceLength) {
        return AttentionType.anySatisfied(
                problem.getAttentionTypes(commId, invId), position, sequenceLength);
    }

    protected boolean isGroupSatisfied(int commId1, int commId2) {
        if (commId1 < 0) {
            return true;
        }
        if (commId2 < 0) {
            return true;
        }
        if (commId1 == commId2) {
            return true;
        }
        return problem.getCommercial(commId1).getGroup()
                != problem.getCommercial(commId2).getGroup();
    }

    protected boolean isGroupSatisfied(int leftCommId, int middleCommId, int rightCommId) {
        return isGroupSatisfied(leftCommId, middleCommId)
                && isGroupSatisfied(middleCommId, rightCommId);
    }

    protected int commAt(int[] sequence, int pos) {
        if (pos < 0 || pos >= sequence.length) {
            return -1;
        }
        return sequence[pos];
    }

    protected boolean isDurationSatisfied(int invId, int deltaDuration) {
        return solution.getTotalInvDuration()[invId] + deltaDuration
                <= problem.getInventory(invId).getDuration();
    }

    protected boolean isHourlyLimitSatisfied(int currentHourDuration, int deltaDuration) {
        return currentHourDuration + deltaDuration <= Problem.HOURLY_BROADCAST_LIMIT;
    }

    protected boolean isCommercialCountSatisfied(int invId) {
        return solution.getSequences()[invId].length
                < problem.getInventory(invId).getMaxCommercialCount();
    }

    protected double calculateRevenueChange(int invId, int fromPosition, int timeDelta) {
        return calculateRevenueChange(
                invId, fromPosition, solution.getSequences()[invId].length - 1, timeDelta);
    }

    protected double calculateRevenueChange(
            int invId, int fromPosition, int lastPosition, int timeDelta) {
        if (timeDelta == 0) {
            return 0.0;
        }
        var sequences = solution.getSequences()[invId];
        var startTimes = solution.getStartTimes()[invId];
        var revenues = solution.getRevenues()[invId];
        var delta = 0.0;
        for (int pos = fromPosition; pos <= lastPosition; pos++) {
            var newRevenue = problem.getRevenue(sequences[pos], invId, startTimes[pos] + timeDelta);
            delta += newRevenue - revenues[pos];
        }
        return delta;
    }
}
