package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import data.enums.ATTENTION;
import runParameters.LoopSetup;

import java.util.List;

@SuppressWarnings("RedundantIfStatement")
public class InsertMove implements IMove {
    private final Solution solution;
    private final Commercial c;
    private final Inventory i;
    private final int n;

    private final double inventoryUtilization;
    private final List<SolutionData> solutionDataList;
    private final int lastIndexOfInventory;
    private final double cStartTime;

    private final double[] totalCommercialDurationOfHour;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;

    public InsertMove(Solution solution, Commercial c, Inventory i, int n,
                      double[] totalCommercialDurationOfHour) {
        this.solution = solution;
        this.c = c;
        this.i = i;
        this.n = n;
        this.solutionDataList = solution.solution.get(i.getId());
        this.lastIndexOfInventory = solutionDataList.size() - 1;

        this.inventoryUtilization = lastIndexOfInventory >= 0 ? solutionDataList.get(lastIndexOfInventory).getEndTime() : 0;
        if (lastIndexOfInventory >= 0) {
            this.cStartTime = n == 0 ? 0 : solutionDataList.get(n - 1).getEndTime();
        } else this.cStartTime = 0;

        this.totalCommercialDurationOfHour = totalCommercialDurationOfHour;
    }

    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();

        var newSolutionList = newSolution.solution.get(i.getId());
        var newSolutionData = new SolutionData(c, i);

        newSolution.addSolutionData(i, newSolutionData, n);

        for (var l = n + 1; l <= lastIndexOfInventory + 1; l++) {
            var solutionData = newSolutionList.get(l);
            MoveUtils.updateSolutionData(
                    solutionData,
                    i,
                    c.getDuration(),
                    l
            );
        }

        newSolutionData.update(
                i,
                c.getRevenue(i, cStartTime),
                cStartTime,
                n
        );

        newSolution.revenue += calculateRevenueGain();

        if (LoopSetup.isDebug) {
            Utils.feasibilityCheck(newSolution);
            this.checkFeasibility();
        }

        return newSolution;
    }

    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return revenueGain;

        double gain = 0;

        var shift = c.getDuration();
        for (var l = n; l <= lastIndexOfInventory; l++) {
            var solutionData = solutionDataList.get(l);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    i,
                    shift
            );
        }

        gain += c.getRevenue(i, cStartTime);

        isRevenueGainCalculated = true;
        revenueGain = gain;

        return revenueGain;
    }

    public boolean checkFeasibility() {
        if (isFeasibilityChecked) return isFeasible;

        if (!checkGeneral()) {
            isFeasible = false;
            return false;
        }

        if (!conditionalCheck()) {
            isFeasible = false;
            return false;
        }

        isFeasibilityChecked = true;
        isFeasible = true;

        return true;
    }

    private boolean checkGeneral() {
        for (var l = n; l <= lastIndexOfInventory; l++) {
            var solutionData = solutionDataList.get(l);
            if (!MoveUtils.isAttentionSatisfied(
                    solutionData.getCommercial(),
                    i,
                    l + 1,
                    solutionData.getStartTime() + c.getDuration(),
                    lastIndexOfInventory + 1
            )) return false;
        }

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n - 1 >= 0 ? solutionDataList.get(n - 1).getCommercial() : null,
                c,
                n <= lastIndexOfInventory ? solutionDataList.get(n).getCommercial() : null
        )) return false;

        var doesDurationNotExceed = inventoryUtilization + c.getDuration() <= i.getDuration();
        if (!doesDurationNotExceed) return false;

        var doesHourlyDurationNotExceed = totalCommercialDurationOfHour[i.getHour()] + c.getDuration() <= 720;
        if (!doesHourlyDurationNotExceed) return false;

        return true;
    }

    private boolean conditionalCheck() {
        if (n == lastIndexOfInventory + 1 && lastIndexOfInventory >= 0) {
            var lastSolutionData = solutionDataList.get(lastIndexOfInventory);
            var commercial = lastSolutionData.getCommercial();
            var inventory = lastSolutionData.getInventory();

            var isAttentionLast = commercial.getAttentionMap().get(inventory) == ATTENTION.LAST;
            if (isAttentionLast) return false;

            if (!MoveUtils.isAttentionSatisfied(
                    c,
                    i,
                    n,
                    solutionDataList.get(lastIndexOfInventory).getEndTime(),
                    lastIndexOfInventory + 1
            )) return false;
        }

        if (n != lastIndexOfInventory + 1) {
            if (!MoveUtils.isAttentionSatisfied(
                    c,
                    i,
                    n,
                    solutionDataList.get(n).getStartTime(),
                    lastIndexOfInventory + 1
            )) return false;
        }

        return true;
    }

    public Solution getSolution() {
        return solution;
    }
}
