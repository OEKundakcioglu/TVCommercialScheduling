package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import data.enums.ATTENTION;
import runParameters.LoopSetup;

import java.util.List;

@SuppressWarnings("RedundantIfStatement")
public class TransferMove implements IMove {

    private final Solution solution;
    private final Inventory n1Inventory;
    private final Inventory n2Inventory;

    private final int n1;
    private final int n2;
    private final Commercial n1Commercial;
    private final SolutionData n1SolutionData;
    private final int startTimeAtN2;
    private final List<SolutionData> n1InventoryList;
    private final List<SolutionData> n2InventoryList;
    private final int lastIndexOfN1;
    private final int lastIndexOfN2;
    private final double n2Utilization;

    private final double[] totalCommercialDurationOfHour;
    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;



    public TransferMove(Solution solution, Inventory n1Inventory, Inventory n2Inventory,
                        int n1, int n2, double[] totalCommercialDurationOfHour) {
        this.solution = solution;
        this.n1Inventory = n1Inventory;
        this.n2Inventory = n2Inventory;
        this.totalCommercialDurationOfHour = totalCommercialDurationOfHour;

        this.n1 = n1;
        this.n2 = n2;
        this.n1InventoryList = solution.solution.get(n1Inventory.getId());
        this.n2InventoryList = solution.solution.get(n2Inventory.getId());
        this.lastIndexOfN2 = n2InventoryList.size() - 1;
        this.lastIndexOfN1 = n1InventoryList.size() - 1;
        this.n1SolutionData = n1InventoryList.get(n1);
        this.n1Commercial = n1SolutionData.getCommercial();

        this.startTimeAtN2 = n2 > 0 ? n2InventoryList.get(n2 - 1).getEndTime() : 0;

        this.n2Utilization = n2InventoryList.isEmpty() ? 0 : n2InventoryList.getLast().getEndTime();
    }

    @Override
    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();

        var fromInvSolutionList = newSolution.solution.get(n1Inventory.getId());
        var toInvSolutionList = newSolution.solution.get(n2Inventory.getId());

        var transferedSolutionData = newSolution.getSolutionData(n1Commercial);

        newSolution.removeSolutionData(n1Inventory, transferedSolutionData);
        newSolution.addSolutionData(n2Inventory, transferedSolutionData, n2);

        for (var n = n1; n <= lastIndexOfN1-1; n++){
            var solutionData = fromInvSolutionList.get(n);
            MoveUtils.updateSolutionData(
                    solutionData,
                    n1Inventory,
                    -n1Commercial.getDuration(),
                    n
            );
        }

        for (var n = n2+1; n <= lastIndexOfN2+1; n++){
            var solutionData = toInvSolutionList.get(n);
            MoveUtils.updateSolutionData(
                    solutionData,
                    n2Inventory,
                    n1Commercial.getDuration(),
                    n
            );
        }

        transferedSolutionData.update(
                n2Inventory,
                n1Commercial.getRevenue(n2Inventory, startTimeAtN2),
                startTimeAtN2,
                n2
        );

        newSolution.revenue += (int) calculateRevenueGain();

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }

    @Override
    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return this.revenueGain;

        double gain = 0;
        for (var n = n1+1; n <= lastIndexOfN1; n++){
            var solutionData = n1InventoryList.get(n);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    n1Inventory,
                    -n1Commercial.getDuration()
            );
        }

        for (var n = n2; n <= lastIndexOfN2; n++){
            var solutionData = n2InventoryList.get(n);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    n2Inventory,
                    n1Commercial.getDuration()
            );
        }

        gain += n1Commercial.getRevenue(n2Inventory, startTimeAtN2) - n1SolutionData.getRevenue();

        isRevenueGainCalculated = true;
        this.revenueGain = gain;
        return this.revenueGain;
    }

    @Override
    public boolean checkFeasibility() {
        if (isFeasibilityChecked) return isFeasible;

        if (!conditionalCheck()) { //
            isFeasible = false;
            return false;
        }

        if (!generalCheck()) { //
            isFeasible = false;
            return false;
        }

        isFeasibilityChecked = true;
        this.isFeasible = true;
        return true;
    }

    private boolean generalCheck(){
        if (n2Utilization + n1Commercial.getDuration() > n2Inventory.getDuration())
            return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n1 - 1 >= 0 ? n1InventoryList.get(n1 - 1).getCommercial() : null,
                n1 + 1 <= lastIndexOfN1 ? n1InventoryList.get(n1 + 1).getCommercial() : null
                                                  )) return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n2 - 1 >= 0 ? n2InventoryList.get(n2 - 1).getCommercial() : null,
                n1Commercial,
                n2 <= lastIndexOfN2 ? n2InventoryList.get(n2).getCommercial() : null
                                                  )) return false;

        for (var i = n2; i <= lastIndexOfN2; i++) {
            var solutionData = n2InventoryList.get(i);
            if (!MoveUtils.isAttentionSatisfied(
                    solutionData.getCommercial(),
                    n2Inventory,
                    i + 1,
                    solutionData.getStartTime() + n1Commercial.getDuration(),
                    lastIndexOfN2 + 1)) return false;
        }

        return true;
    }

    private boolean conditionalCheck(){
        if (0 < n2 && n2 == lastIndexOfN2 + 1) {
            var solData = n2InventoryList.getLast();
            if (solData.getCommercial().getAttentionMapArray()[n2Inventory.getId()] == ATTENTION.LAST) return false;

            if (!MoveUtils.isAttentionSatisfied(
                    n1Commercial,
                    n2Inventory,
                    n2,
                    n2Utilization,
                    lastIndexOfN2 + 1)) return false;
        }

        if (n2 == 0) {
            if (!MoveUtils.isAttentionSatisfied(
                    n1Commercial,
                    n2Inventory,
                    n2,
                    0,
                    lastIndexOfN2 + 1)) return false;
        }

        if (n2 > 0 && n2 < lastIndexOfN2 + 1){
            if (!MoveUtils.isAttentionSatisfied(
                    n1Commercial,
                    n2Inventory,
                    n2,
                    n2InventoryList.get(n2).getStartTime(),
                    lastIndexOfN2 + 1)) return false;
        }

        if (n1Inventory.getHour() != n2Inventory.getHour()){
            var doesViolateHourlyLimit = totalCommercialDurationOfHour[n2Inventory.getHour()] +
                    n1Commercial.getDuration() > 720;
            if (doesViolateHourlyLimit) return false;
        }

        return true;
    }

    public Solution getSolution() {
        return solution;
    }
}
