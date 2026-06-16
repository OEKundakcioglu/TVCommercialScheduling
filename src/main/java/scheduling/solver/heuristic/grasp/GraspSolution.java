package scheduling.solver.heuristic.grasp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;

// Mutable solution state for the GRASP heuristic.
// Per-inventory arrays are the source of truth.
// - sequences[invId]: ordered array of commercial IDs assigned to that inventory
// - startTimes[invId][pos]: start time of the commercial at that position
// - revenues[invId][pos]: revenue of the commercial at that position (from revenueMatrix)
// - totalRevenue: sum of all revenues across all inventories
// - totalDurationOfHour[hourIndex]: total commercial duration broadcast in that hour
//   (used for hourly broadcast limit feasibility checks)
// - totalInvDuration[invId]: total commercial duration assigned to that inventory
//   (used for inventory duration capacity feasibility checks)
//
// When a move is applied, it returns a new GraspSolution with the affected
// inventory's arrays rebuilt and caches updated.
@Getter
@RequiredArgsConstructor
public class GraspSolution {

    private final int[][] sequences;
    private final int[][] startTimes;
    private final double[][] revenues;
    private final double totalRevenue;
    private final int[] totalDurationOfHour;
    private final int[] totalInvDuration;
    private final int[] assignedInvId;
    private final int[] assignedPos;

    public Builder toBuilder(Problem problem) {
        return new Builder(this, problem);
    }

    public static class Builder {

        private final GraspSolution original;
        private final Problem problem;

        private int[][] sequences;
        private int[][] startTimes;
        private double[][] revenues;

        private int[] totalInvDuration;
        private int[] totalDurationOfHour;

        private int[] assignedInvId;
        private int[] assignedPos;

        private double revenueDelta;

        Builder(GraspSolution original, Problem problem) {
            this.original = original;
            this.problem = problem;
            clonePerInventoryArrays();
            cloneDurationArrays();
            cloneAssignmentArrays();
        }

        public Builder replaceSequence(int invId, int[] newSequence, int unchangedPositions) {
            // Clear assignments for commercials still belonging to this inventory
            var oldSequence = sequences[invId];
            for (int commId : oldSequence) {
                if (assignedInvId[commId] == invId) {
                    assignedInvId[commId] = -1;
                    assignedPos[commId] = -1;
                }
            }

            // Set assignments for the new sequence
            sequences[invId] = newSequence;
            for (int pos = 0; pos < newSequence.length; pos++) {
                assignedInvId[newSequence[pos]] = invId;
                assignedPos[newSequence[pos]] = pos;
            }

            // Copy unchanged positions, rebuild the rest
            var newStartTimes = new int[newSequence.length];
            var newRevenues = new double[newSequence.length];
            int copyLen = Math.min(unchangedPositions, original.startTimes[invId].length);
            if (copyLen > 0) {
                System.arraycopy(original.startTimes[invId], 0, newStartTimes, 0, copyLen);
                System.arraycopy(original.revenues[invId], 0, newRevenues, 0, copyLen);
            }
            rebuildStartTimesAndRevenues(
                    newSequence, invId, newStartTimes, newRevenues, unchangedPositions);
            startTimes[invId] = newStartTimes;
            revenues[invId] = newRevenues;

            return this;
        }

        public Builder addDuration(int invId, int delta) {
            if (delta == 0) {
                return this;
            }
            totalInvDuration[invId] += delta;
            int hour = problem.getInventory(invId).getHour();
            totalDurationOfHour[hour] += delta;
            return this;
        }

        public Builder addRevenue(double delta) {
            revenueDelta += delta;
            return this;
        }

        public GraspSolution build() {
            return new GraspSolution(
                    sequences,
                    startTimes,
                    revenues,
                    original.totalRevenue + revenueDelta,
                    totalDurationOfHour,
                    totalInvDuration,
                    assignedInvId,
                    assignedPos);
        }

        private void clonePerInventoryArrays() {
            sequences = original.sequences.clone();
            startTimes = original.startTimes.clone();
            revenues = original.revenues.clone();
        }

        private void cloneDurationArrays() {
            totalInvDuration = original.totalInvDuration.clone();
            totalDurationOfHour = original.totalDurationOfHour.clone();
        }

        private void cloneAssignmentArrays() {
            assignedInvId = original.assignedInvId.clone();
            assignedPos = original.assignedPos.clone();
        }

        private void rebuildStartTimesAndRevenues(
                int[] sequence, int invId, int[] startTimes, double[] revenues, int fromPos) {
            int currentTime;
            if (fromPos == 0) {
                currentTime = 0;
            } else {
                currentTime =
                        startTimes[fromPos - 1]
                                + problem.getCommercial(sequence[fromPos - 1]).getDuration();
            }
            for (int pos = fromPos; pos < sequence.length; pos++) {
                startTimes[pos] = currentTime;
                revenues[pos] = problem.getRevenue(sequence[pos], invId, currentTime);
                currentTime += problem.getCommercial(sequence[pos]).getDuration();
            }
        }
    }
}
