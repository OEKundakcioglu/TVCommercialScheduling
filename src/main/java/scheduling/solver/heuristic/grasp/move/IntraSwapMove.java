package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class IntraSwapMove extends Move {

    private final int invId;
    private final int pos1;
    private final int pos2;

    public IntraSwapMove(Problem problem, GraspSolution solution, int invId, int pos1, int pos2) {
        super(problem, solution);
        this.invId = invId;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    public boolean checkFeasibility() {
        var sequence = solution.getSequences()[invId];
        var n = sequence.length;
        var comm1Id = sequence[pos1];
        var comm2Id = sequence[pos2];

        if (!checkAttentionFeasibility(n, comm1Id, comm2Id)) {
            return false;
        }
        return checkGroupFeasibility(sequence, comm1Id, comm2Id);
    }

    @Override
    protected double computeRevenueGain() {
        var sequence = solution.getSequences()[invId];
        var startTimes = solution.getStartTimes()[invId];
        var revenues = solution.getRevenues()[invId];
        var comm1Id = sequence[pos1];
        var comm2Id = sequence[pos2];
        var shift =
                problem.getCommercial(comm2Id).getDuration()
                        - problem.getCommercial(comm1Id).getDuration();

        var delta = 0.0;

        delta += problem.getRevenue(comm2Id, invId, startTimes[pos1]) - revenues[pos2];
        delta += problem.getRevenue(comm1Id, invId, startTimes[pos2] + shift) - revenues[pos1];
        delta += calculateRevenueChange(invId, pos1 + 1, pos2 - 1, shift);

        return delta;
    }

    @Override
    public GraspSolution apply() {
        return solution.toBuilder(problem)
                .replaceSequence(invId, buildNewSequence(), pos1)
                .addRevenue(calculateRevenueGain())
                .build();
    }

    private boolean checkAttentionFeasibility(int n, int comm1Id, int comm2Id) {
        if (!isAttentionSatisfied(comm1Id, invId, pos2, n)) {
            return false;
        }
        return isAttentionSatisfied(comm2Id, invId, pos1, n);
    }

    private boolean checkGroupFeasibility(int[] sequence, int comm1Id, int comm2Id) {
        if (pos2 == pos1 + 1) {
            if (!isGroupSatisfied(commAt(sequence, pos1 - 1), comm2Id, comm1Id)) {
                return false;
            }
            return isGroupSatisfied(comm2Id, comm1Id, commAt(sequence, pos2 + 1));
        }
        if (!isGroupSatisfied(commAt(sequence, pos1 - 1), comm2Id, commAt(sequence, pos1 + 1))) {
            return false;
        }
        return isGroupSatisfied(commAt(sequence, pos2 - 1), comm1Id, commAt(sequence, pos2 + 1));
    }

    private int[] buildNewSequence() {
        var oldSequence = solution.getSequences()[invId];
        var newSequence = oldSequence.clone();
        newSequence[pos1] = oldSequence[pos2];
        newSequence[pos2] = oldSequence[pos1];
        return newSequence;
    }
}
