package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import runParameters.LoopSetup;

import java.util.List;

@SuppressWarnings("RedundantIfStatement")
public class OutOfPoolSwapMove implements IMove{

    private final Commercial c;
    private final Inventory i;
    private final Solution solution;
    private final double[] totalHourlyBroadcastMap;

    private final Commercial inPoolCommercial;
    private final SolutionData inPoolSolutionData;

    private final List<SolutionData> solutionDataList;
    private final int lastIndexOfInventory;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;
    private final int cStartTime;

    private final int n;

    public OutOfPoolSwapMove(Solution solution,
                             int n,
                             Commercial c,
                             Inventory i,
                                double[] totalHourlyBroadcastMap) {
        this.solution = solution;
        this.c = c;
        this.i = i;
        this.solutionDataList = solution.solution.get(i.getId());
        this.lastIndexOfInventory = solutionDataList.size() - 1;
        this.totalHourlyBroadcastMap = totalHourlyBroadcastMap;

        this.n = n;
        this.inPoolSolutionData = solutionDataList.get(n);
        this.inPoolCommercial = inPoolSolutionData.getCommercial();

        if (lastIndexOfInventory >= 0){
            this.cStartTime = n == 0 ? 0 : solutionDataList.get(n - 1).getEndTime();
        }
        else this.cStartTime = 0;
    }

    @Override
    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();

        var newSolutionList = newSolution.solution.get(i.getId());
        var newSolutionData = new SolutionData(c, i);
        var newToBeRemovedSolData = newSolutionList.get(n);

        newSolution.removeSolutionData(i, newToBeRemovedSolData);
        newSolution.addSolutionData(i, newSolutionData, n);

        var shift = c.getDuration() - inPoolCommercial.getDuration();
        for (var l = n + 1; l <= lastIndexOfInventory; l++){
            var solutionData = newSolutionList.get(l);
            MoveUtils.updateSolutionData(
                    solutionData,
                    i,
                    shift,
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

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }

    @Override
    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return revenueGain;

        double gain = 0;

        var shift = c.getDuration() - inPoolCommercial.getDuration();
        for (var l = n+1; l <= lastIndexOfInventory; l++) {
            var solutionData = solutionDataList.get(l);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    i,
                    shift
            );
        }

        gain += c.getRevenue(i, cStartTime) - inPoolSolutionData.getRevenue();

        isRevenueGainCalculated = true;
        revenueGain = gain;

        return revenueGain;
    }

    @Override
    public boolean checkFeasibility() {
        if (isFeasibilityChecked) return isFeasible;

        if (!checkGeneral()) {
            isFeasible = false;
            return false;
        }

        if (!checkConditional()){
            isFeasible = false;
            return false;
        }

        isFeasibilityChecked = true;
        isFeasible = true;
        return true;
    }

    private boolean checkGeneral() {
        if (!MoveUtils.isAttentionSatisfied(
                c,
                i,
                n,
                inPoolSolutionData.getStartTime(),
                lastIndexOfInventory
        )) return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n - 1 >= 0 ? solutionDataList.get(n - 1).getCommercial() : null,
                c,
                n + 1 <= lastIndexOfInventory ? solutionDataList.get(n + 1).getCommercial() : null
        )) return false;

        return true;
    }

    private boolean checkConditional() {
        if (c.getDuration() > inPoolCommercial.getDuration()) {
            var shift = c.getDuration() - inPoolCommercial.getDuration();
            for (var l = n+1; l <= lastIndexOfInventory; l++){
                var solutionData = solutionDataList.get(l);
                if (!MoveUtils.isAttentionSatisfied(
                        solutionData.getCommercial(),
                        i,
                        l,
                        solutionData.getStartTime() + shift,
                        lastIndexOfInventory
                )) return false;
            }

            var doesNotExceedInventoryDuration = solutionDataList.get(lastIndexOfInventory).getEndTime()
                    + (c.getDuration() - inPoolCommercial.getDuration()) <= i.getDuration();
            if (!doesNotExceedInventoryDuration) return false;

            var doesNotExceedHourlyLimit = totalHourlyBroadcastMap[i.getHour()]
                    + (c.getDuration() - inPoolCommercial.getDuration()) <= 720;
            if (!doesNotExceedHourlyLimit) return false;
        }

        return true;
    }

    public Solution getSolution() {
        return solution;
    }

}
