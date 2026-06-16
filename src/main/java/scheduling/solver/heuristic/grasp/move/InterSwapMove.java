package scheduling.solver.heuristic.grasp.move;

import scheduling.model.Problem;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class InterSwapMove extends Move {

    private final int invId1;
    private final int pos1;
    private final int invId2;
    private final int pos2;

    public InterSwapMove(
            Problem problem, GraspSolution solution, int invId1, int pos1, int invId2, int pos2) {
        super(problem, solution);
        var comm1Dur = problem.getCommercial(solution.getSequences()[invId1][pos1]).getDuration();
        var comm2Dur = problem.getCommercial(solution.getSequences()[invId2][pos2]).getDuration();
        if (comm1Dur > comm2Dur) {
            this.invId1 = invId2;
            this.pos1 = pos2;
            this.invId2 = invId1;
            this.pos2 = pos1;
        } else {
            this.invId1 = invId1;
            this.pos1 = pos1;
            this.invId2 = invId2;
            this.pos2 = pos2;
        }
    }

    @Override
    public boolean checkFeasibility() {
        var seq1 = solution.getSequences()[invId1];
        var seq2 = solution.getSequences()[invId2];
        var comm1Id = seq1[pos1];
        var comm2Id = seq2[pos2];
        var timeDelta =
                problem.getCommercial(comm2Id).getDuration()
                        - problem.getCommercial(comm1Id).getDuration();

        if (!problem.isSuitable(comm1Id, invId2)) {
            return false;
        }
        if (!problem.isSuitable(comm2Id, invId1)) {
            return false;
        }
        if (timeDelta > 0) {
            if (!isDurationSatisfied(invId1, timeDelta)) {
                return false;
            }
            if (!checkHourlyLimitFeasibility(timeDelta)) {
                return false;
            }
        }
        if (!isAttentionSatisfied(comm1Id, invId2, pos2, seq2.length)) {
            return false;
        }
        if (!isAttentionSatisfied(comm2Id, invId1, pos1, seq1.length)) {
            return false;
        }
        return checkGroupFeasibility(seq1, seq2, comm1Id, comm2Id);
    }

    private boolean checkHourlyLimitFeasibility(int timeDelta) {
        var hour1 = problem.getInventory(invId1).getHour();
        var hour2 = problem.getInventory(invId2).getHour();
        if (hour1 == hour2) {
            return true;
        }
        var currentHourDuration = solution.getTotalDurationOfHour()[hour1];
        return isHourlyLimitSatisfied(currentHourDuration, timeDelta);
    }

    private boolean checkGroupFeasibility(int[] seq1, int[] seq2, int comm1Id, int comm2Id) {
        var group1 = problem.getCommercial(comm1Id).getGroup();
        var group2 = problem.getCommercial(comm2Id).getGroup();
        if (group1 == group2) {
            return true;
        }
        if (!isGroupSatisfied(commAt(seq1, pos1 - 1), comm2Id, commAt(seq1, pos1 + 1))) {
            return false;
        }
        return isGroupSatisfied(commAt(seq2, pos2 - 1), comm1Id, commAt(seq2, pos2 + 1));
    }

    @Override
    protected double computeRevenueGain() {
        var seq1 = solution.getSequences()[invId1];
        var seq2 = solution.getSequences()[invId2];
        var startTimes1 = solution.getStartTimes()[invId1];
        var startTimes2 = solution.getStartTimes()[invId2];
        var revenues1 = solution.getRevenues()[invId1];
        var revenues2 = solution.getRevenues()[invId2];
        var comm1Id = seq1[pos1];
        var comm2Id = seq2[pos2];
        var timeDelta =
                problem.getCommercial(comm2Id).getDuration()
                        - problem.getCommercial(comm1Id).getDuration();

        var delta = 0.0;
        delta += problem.getRevenue(comm1Id, invId2, startTimes2[pos2]) - revenues1[pos1];
        delta += problem.getRevenue(comm2Id, invId1, startTimes1[pos1]) - revenues2[pos2];

        if (timeDelta != 0) {
            delta += calculateRevenueChange(invId1, pos1 + 1, timeDelta);
            delta += calculateRevenueChange(invId2, pos2 + 1, -timeDelta);
        }

        return delta;
    }

    @Override
    public GraspSolution apply() {
        var seq1 = solution.getSequences()[invId1];
        var seq2 = solution.getSequences()[invId2];
        var comm1Id = seq1[pos1];
        var comm2Id = seq2[pos2];
        var timeDelta =
                problem.getCommercial(comm2Id).getDuration()
                        - problem.getCommercial(comm1Id).getDuration();
        var newSeq1 = seq1.clone();
        newSeq1[pos1] = comm2Id;
        var newSeq2 = seq2.clone();
        newSeq2[pos2] = comm1Id;
        return solution.toBuilder(problem)
                .replaceSequence(invId1, newSeq1, pos1)
                .replaceSequence(invId2, newSeq2, pos2)
                .addDuration(invId1, timeDelta)
                .addDuration(invId2, -timeDelta)
                .addRevenue(calculateRevenueGain())
                .build();
    }
}
