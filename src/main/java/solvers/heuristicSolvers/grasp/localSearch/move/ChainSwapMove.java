package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import runParameters.LoopSetup;

import java.util.List;

/**
 * 3-way cyclic swap move.
 * c1 moves to i2's position, c2 moves to i3's position, c3 moves to i1's position.
 * This helps escape local optima that 2-swap cannot escape.
 */
public class ChainSwapMove implements IMove {

    private final Solution solution;
    private final double[] totalCommercialDurationOfHour;

    private final Inventory i1, i2, i3;
    private final Commercial c1, c2, c3;
    private final SolutionData sd1, sd2, sd3;
    private final List<SolutionData> list1, list2, list3;
    private final int n1, n2, n3;
    private final int last1, last2, last3;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;

    /**
     * Creates a 3-way chain swap move.
     *
     * @param solution                      The current solution
     * @param i1                            First inventory
     * @param n1                            Position in first inventory
     * @param i2                            Second inventory
     * @param n2                            Position in second inventory
     * @param i3                            Third inventory
     * @param n3                            Position in third inventory
     * @param totalCommercialDurationOfHour Hourly duration constraints
     */
    public ChainSwapMove(Solution solution,
                         Inventory i1, int n1,
                         Inventory i2, int n2,
                         Inventory i3, int n3,
                         double[] totalCommercialDurationOfHour) {
        this.solution = solution;
        this.totalCommercialDurationOfHour = totalCommercialDurationOfHour;

        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;

        this.list1 = solution.solution.get(i1.getId());
        this.list2 = solution.solution.get(i2.getId());
        this.list3 = solution.solution.get(i3.getId());

        this.sd1 = list1.get(n1);
        this.sd2 = list2.get(n2);
        this.sd3 = list3.get(n3);

        this.c1 = sd1.getCommercial();
        this.c2 = sd2.getCommercial();
        this.c3 = sd3.getCommercial();

        this.last1 = list1.size() - 1;
        this.last2 = list2.size() - 1;
        this.last3 = list3.size() - 1;
    }

    @Override
    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();

        var newList1 = newSolution.solution.get(i1.getId());
        var newList2 = newSolution.solution.get(i2.getId());
        var newList3 = newSolution.solution.get(i3.getId());

        var newSd1 = newList1.get(n1);
        var newSd2 = newList2.get(n2);
        var newSd3 = newList3.get(n3);

        // c1 -> i2 position, c2 -> i3 position, c3 -> i1 position
        newList1.set(n1, newSd3);
        newList2.set(n2, newSd1);
        newList3.set(n3, newSd2);

        // Calculate duration shifts for each inventory
        int shift1 = c3.getDuration() - c1.getDuration(); // c3 replaces c1 in i1
        int shift2 = c1.getDuration() - c2.getDuration(); // c1 replaces c2 in i2
        int shift3 = c2.getDuration() - c3.getDuration(); // c2 replaces c3 in i3

        // Update subsequent positions in i1
        for (int n = n1 + 1; n <= last1; n++) {
            var sd = newList1.get(n);
            MoveUtils.updateSolutionData(sd, i1, shift1, n);
        }

        // Update subsequent positions in i2
        for (int n = n2 + 1; n <= last2; n++) {
            var sd = newList2.get(n);
            MoveUtils.updateSolutionData(sd, i2, shift2, n);
        }

        // Update subsequent positions in i3
        for (int n = n3 + 1; n <= last3; n++) {
            var sd = newList3.get(n);
            MoveUtils.updateSolutionData(sd, i3, shift3, n);
        }

        // Update the swapped commercials themselves
        // c3 now in i1 at position n1 with sd1's original start time
        double newRev3 = c3.getRevenue(i1, sd1.getStartTime());
        newSd3.update(i1, newRev3, sd1.getStartTime(), n1);

        // c1 now in i2 at position n2 with sd2's original start time
        double newRev1 = c1.getRevenue(i2, sd2.getStartTime());
        newSd1.update(i2, newRev1, sd2.getStartTime(), n2);

        // c2 now in i3 at position n3 with sd3's original start time
        double newRev2 = c2.getRevenue(i3, sd3.getStartTime());
        newSd2.update(i3, newRev2, sd3.getStartTime(), n3);

        newSolution.revenue += (int) calculateRevenueGain();

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }

    @Override
    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return revenueGain;

        double gain = 0;

        // Duration shifts
        int shift1 = c3.getDuration() - c1.getDuration();
        int shift2 = c1.getDuration() - c2.getDuration();
        int shift3 = c2.getDuration() - c3.getDuration();

        // Revenue changes for subsequent positions in each inventory
        for (int n = n1 + 1; n <= last1; n++) {
            gain += MoveUtils.calculateRevenueChange(list1.get(n), i1, shift1);
        }
        for (int n = n2 + 1; n <= last2; n++) {
            gain += MoveUtils.calculateRevenueChange(list2.get(n), i2, shift2);
        }
        for (int n = n3 + 1; n <= last3; n++) {
            gain += MoveUtils.calculateRevenueChange(list3.get(n), i3, shift3);
        }

        // Revenue changes for the swapped commercials
        gain += c3.getRevenue(i1, sd1.getStartTime()) - sd1.getRevenue(); // c3 in i1
        gain += c1.getRevenue(i2, sd2.getStartTime()) - sd2.getRevenue(); // c1 in i2
        gain += c2.getRevenue(i3, sd3.getStartTime()) - sd3.getRevenue(); // c2 in i3

        this.revenueGain = gain;
        isRevenueGainCalculated = true;
        return revenueGain;
    }

    @Override
    public boolean checkFeasibility() {
        if (isFeasibilityChecked) return isFeasible;

        if (!checkHourlyConstraints()) {
            isFeasible = false;
            return false;
        }

        if (!checkDurationConstraints()) {
            isFeasible = false;
            return false;
        }

        if (!checkSuitabilityConstraints()) {
            isFeasible = false;
            return false;
        }

        if (!checkGroupConstraints()) {
            isFeasible = false;
            return false;
        }

        if (!checkAttentionConstraints()) {
            isFeasible = false;
            return false;
        }

        isFeasibilityChecked = true;
        isFeasible = true;
        return true;
    }

    private boolean checkHourlyConstraints() {
        // Check hourly duration constraints for each distinct hour involved
        int h1 = i1.getHour();
        int h2 = i2.getHour();
        int h3 = i3.getHour();

        // Calculate net duration change per hour
        double delta1 = c3.getDuration() - c1.getDuration(); // i1 gets c3 instead of c1
        double delta2 = c1.getDuration() - c2.getDuration(); // i2 gets c1 instead of c2
        double delta3 = c2.getDuration() - c3.getDuration(); // i3 gets c2 instead of c3

        // Apply deltas to hours
        double change1 = delta1;
        double change2 = delta2;
        double change3 = delta3;

        // If inventories share hours, combine their changes
        if (h1 == h2) change1 += delta2;
        if (h1 == h3) change1 += delta3;
        if (h2 == h3 && h2 != h1) change2 += delta3;

        // Check the constraint (720 seconds max per hour)
        if (change1 != 0 && totalCommercialDurationOfHour[h1] + change1 > 720) return false;
        if (h2 != h1 && change2 != 0 && totalCommercialDurationOfHour[h2] + change2 > 720) return false;
        if (h3 != h1 && h3 != h2 && change3 != 0 && totalCommercialDurationOfHour[h3] + change3 > 720) return false;

        return true;
    }

    private boolean checkDurationConstraints() {
        // Check that each inventory doesn't exceed its duration limit
        int shift1 = c3.getDuration() - c1.getDuration();
        int shift2 = c1.getDuration() - c2.getDuration();
        int shift3 = c2.getDuration() - c3.getDuration();

        if (list1.get(last1).getEndTime() + shift1 > i1.getDuration()) return false;
        if (list2.get(last2).getEndTime() + shift2 > i2.getDuration()) return false;
        if (list3.get(last3).getEndTime() + shift3 > i3.getDuration()) return false;

        return true;
    }

    private boolean checkSuitabilityConstraints() {
        // Each commercial must be suitable for its new inventory
        if (!c3.isInventorySuitable(i1)) return false;
        if (!c1.isInventorySuitable(i2)) return false;
        if (!c2.isInventorySuitable(i3)) return false;
        return true;
    }

    private boolean checkGroupConstraints() {
        // c3 in i1 at position n1
        if (!MoveUtils.isGroupConstraintsSatisfied(
                n1 > 0 ? list1.get(n1 - 1).getCommercial() : null,
                c3,
                n1 < last1 ? list1.get(n1 + 1).getCommercial() : null)) {
            return false;
        }

        // c1 in i2 at position n2
        if (!MoveUtils.isGroupConstraintsSatisfied(
                n2 > 0 ? list2.get(n2 - 1).getCommercial() : null,
                c1,
                n2 < last2 ? list2.get(n2 + 1).getCommercial() : null)) {
            return false;
        }

        // c2 in i3 at position n3
        if (!MoveUtils.isGroupConstraintsSatisfied(
                n3 > 0 ? list3.get(n3 - 1).getCommercial() : null,
                c2,
                n3 < last3 ? list3.get(n3 + 1).getCommercial() : null)) {
            return false;
        }

        return true;
    }

    private boolean checkAttentionConstraints() {
        // Check attention for the swapped commercials
        if (!MoveUtils.isAttentionSatisfied(c3, i1, n1, sd1.getStartTime(), last1)) return false;
        if (!MoveUtils.isAttentionSatisfied(c1, i2, n2, sd2.getStartTime(), last2)) return false;
        if (!MoveUtils.isAttentionSatisfied(c2, i3, n3, sd3.getStartTime(), last3)) return false;

        // Check attention for shifted commercials in each inventory
        int shift1 = c3.getDuration() - c1.getDuration();
        for (int n = n1 + 1; n <= last1; n++) {
            var sd = list1.get(n);
            if (!MoveUtils.isAttentionSatisfied(sd.getCommercial(), i1, n,
                    sd.getStartTime() + shift1, last1)) {
                return false;
            }
        }

        int shift2 = c1.getDuration() - c2.getDuration();
        for (int n = n2 + 1; n <= last2; n++) {
            var sd = list2.get(n);
            if (!MoveUtils.isAttentionSatisfied(sd.getCommercial(), i2, n,
                    sd.getStartTime() + shift2, last2)) {
                return false;
            }
        }

        int shift3 = c2.getDuration() - c3.getDuration();
        for (int n = n3 + 1; n <= last3; n++) {
            var sd = list3.get(n);
            if (!MoveUtils.isAttentionSatisfied(sd.getCommercial(), i3, n,
                    sd.getStartTime() + shift3, last3)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Solution getSolution() {
        return solution;
    }
}
