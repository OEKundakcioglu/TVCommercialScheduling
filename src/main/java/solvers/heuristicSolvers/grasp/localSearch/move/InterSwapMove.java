package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import runParameters.LoopSetup;

import java.util.List;

@SuppressWarnings("RedundantIfStatement")
public class InterSwapMove implements IMove {

    private final Solution solution;

    private final double[] totalCommercialDurationOfHour;

    private final Inventory i1;
    private final Inventory i2;
    private final Commercial c1;
    private final Commercial c2;
    private final SolutionData c1SolutionData;
    private final SolutionData c2SolutionData;
    private final List<SolutionData> solutionDataList1;
    private final List<SolutionData> solutionDataList2;
    private final int n1;
    private final int n2;
    private final int lastIndexOfInventory1;
    private final int lastIndexOfInventory2;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;


    public InterSwapMove(Solution solution,
                            Inventory i1, int n1,
                            Inventory i2, int n2,
                            double[] totalCommercialDurationOfHour) {
        this.solution = solution;

        this.totalCommercialDurationOfHour = totalCommercialDurationOfHour;

        this.solutionDataList1 = solution.solution.get(i1.getId());
        this.solutionDataList2 = solution.solution.get(i2.getId());
        this.c1SolutionData = solutionDataList1.get(n1);
        this.c2SolutionData = solutionDataList2.get(n2);
        this.c1 = c1SolutionData.getCommercial();
        this.c2 = c2SolutionData.getCommercial();
        this.i1 = i1;
        this.i2 = i2;
        this.n1 = n1;
        this.n2 = n2;
        this.lastIndexOfInventory1 = solutionDataList1.size() - 1;
        this.lastIndexOfInventory2 = solutionDataList2.size() - 1;

        assert c1.getDuration() <= c2.getDuration();
    }

    @Override
    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();

        var newSolutionDataList1 = newSolution.solution.get(i1.getId());
        var newSolutionDataList2 = newSolution.solution.get(i2.getId());

        var newSolutionData1 = newSolutionDataList1.get(n1);
        var newSolutionData2 = newSolutionDataList2.get(n2);

        newSolutionDataList1.set(n1, newSolutionData2);
        newSolutionDataList2.set(n2, newSolutionData1);

        var shiftN1 = c2.getDuration() - c1.getDuration();
        for (var n = n1 + 1; n <= lastIndexOfInventory1; n++){
            var solutionData = newSolutionDataList1.get(n);
            MoveUtils.updateSolutionData(
                    solutionData,
                    i1,
                    shiftN1,
                    n
            );
        }

        var shiftN2 = c1.getDuration() - c2.getDuration();
        for (var n = n2 + 1; n <= lastIndexOfInventory2; n++){
            var solutionData = newSolutionDataList2.get(n);
            MoveUtils.updateSolutionData(
                    solutionData,
                    i2,
                    shiftN2,
                    n
            );
        }

        var newSolutionData1Revenue = c1.getRevenue(i2, c2SolutionData.getStartTime());
        var newSolutionData1StartTime = c2SolutionData.getStartTime();
        newSolutionData1.update(i2, newSolutionData1Revenue, newSolutionData1StartTime, n2);

        var newSolutionData2Revenue = c2.getRevenue(i1, c1SolutionData.getStartTime());
        var newSolutionData2StartTime = c1SolutionData.getStartTime();
        newSolutionData2.update(i1, newSolutionData2Revenue, newSolutionData2StartTime, n1);

        newSolution.revenue += calculateRevenueGain();

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }

    @Override
    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return revenueGain;

        double gain = 0;

        var shiftN1 = c2.getDuration() - c1.getDuration();
        for (var n = n1 + 1; n <= lastIndexOfInventory1; n++){
            var solutionData = solutionDataList1.get(n);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    i1,
                    shiftN1
            );
        }

        var shiftN2 = c1.getDuration() - c2.getDuration();
        for (var n = n2 + 1; n <= lastIndexOfInventory2; n++){
            var solutionData = solutionDataList2.get(n);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    i2,
                    shiftN2
            );
        }

        gain += c1.getRevenue(i2, c2SolutionData.getStartTime()) - c1SolutionData.getRevenue();
        gain += c2.getRevenue(i1, c1SolutionData.getStartTime()) - c2SolutionData.getRevenue();

        this.revenueGain = gain;
        isRevenueGainCalculated = true;

        return revenueGain;
    }

    @Override
    public boolean checkFeasibility(){
        if (isFeasibilityChecked) return isFeasible;

        if (!generalCheck()){
            isFeasible = false;
            return false;
        }

        if (!conditionalCheck()){
            isFeasible = false;
            return false;
        }

        isFeasibilityChecked = true;
        isFeasible = true;
        return true;
    }

    private boolean generalCheck(){
        for (var n = n1 + 1; n <= lastIndexOfInventory1; n++){
            var solutionData = solutionDataList1.get(n);
            if (!MoveUtils.isAttentionSatisfied(
                    solutionData.getCommercial(),
                    i1,
                    n,
                    solutionData.getStartTime() + (c2.getDuration() - c1.getDuration()),
                    lastIndexOfInventory1
            )) return false;
        }

        if (!MoveUtils.isAttentionSatisfied(
                c1,
                i2,
                n2,
                c2SolutionData.getStartTime(),
                lastIndexOfInventory2
        )) return false;

        if (!MoveUtils.isAttentionSatisfied(
                c2,
                i1,
                n1,
                c1SolutionData.getStartTime(),
                lastIndexOfInventory1
        )) return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n1 - 1 >= 0 ? solutionDataList1.get(n1 - 1).getCommercial() : null,
                c2,
                n1 + 1 <= lastIndexOfInventory1 ? solutionDataList1.get(n1 + 1).getCommercial() : null
        )) return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n2 - 1 >= 0 ? solutionDataList2.get(n2 - 1).getCommercial() : null,
                c1,
                n2 + 1 <= lastIndexOfInventory2 ? solutionDataList2.get(n2 + 1).getCommercial() : null
        )) return false;

        var doesNotExceedDuration = solutionDataList1.get(lastIndexOfInventory1).getEndTime()
                + (c2.getDuration() - c1.getDuration()) <= i1.getDuration();

        if (!doesNotExceedDuration) return false;

        return true;
    }

    private boolean conditionalCheck(){
        if (i1.getHour() != i2.getHour()){
            var doesNotExceedHourlyDuration = totalCommercialDurationOfHour[i1.getHour()]
                    + c2.getDuration() - c1.getDuration() <= 720;

            if (!doesNotExceedHourlyDuration) return false;
        }

        return true;
    }

    public Solution getSolution() {
        return solution;
    }

}
