package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import data.enums.ATTENTION;
import runParameters.LoopSetup;

@SuppressWarnings("RedundantIfStatement")
public class ShiftMove implements IMove {

    private final Solution solution;
    private final Inventory inventory;
    private final int n2;
    private final Commercial n1Commercial;
    private final Commercial n2Commercial;
    private final SolutionData n1SolutionData;
    private final SolutionData n2SolutionData;
    private final int lastIndexOfInventory;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasibilityChecked = false;
    private boolean isFeasible;

    private final int n1;


    public ShiftMove(Solution solution, Inventory inventory, int n2, int n1) {
        this.solution = solution;
        this.inventory = inventory;
        this.n2 = n2;
        this.n1SolutionData = solution.solution.get(inventory.getId()).get(n1);
        this.n1Commercial = n1SolutionData.getCommercial();
        this.n2SolutionData = solution.solution.get(inventory.getId()).get(n2);
        this.n2Commercial = n2SolutionData.getCommercial();
        this.lastIndexOfInventory = solution.solution.get(inventory.getId()).size() - 1;
        this.n1 = n1;
    }

    @Override
    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();

        var solutionDataList = newSolution.solution.get(inventory.getId());
        var solutionDataToShift = solutionDataList.get(n1);

        solutionDataList.remove(n1);
        solutionDataList.add(n2, solutionDataToShift);

        if (n1 < n2){
            for (var n = n1; n <= n2 - 1; n++){
                var solutionData = solutionDataList.get(n);
                MoveUtils.updateSolutionData(
                        solutionData,
                        inventory,
                        - n1Commercial.getDuration(),
                        n
                );
            }

            var startTime = n2SolutionData.getEndTime() - n1Commercial.getDuration();
            solutionDataToShift.update(
                    inventory,
                    solutionDataToShift.getCommercial().getRevenue(inventory, startTime),
                    startTime,
                    n2
            );
        }

        else {
            for (var n = n2 + 1; n <= n1; n++){
                var solutionData = solutionDataList.get(n);
                MoveUtils.updateSolutionData(
                        solutionData,
                        inventory,
                        n1Commercial.getDuration(),
                        n
                );
            }

            var startTime = n2SolutionData.getStartTime();
            solutionDataToShift.update(
                    inventory,
                    solutionDataToShift.getCommercial().getRevenue(inventory, startTime),
                    startTime,
                    n2
            );
        }

        newSolution.revenue += calculateRevenueGain();

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }

    @Override
    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return revenueGain;

        int calculateRevenueAffectedStartIndex;
        int calculateRevenueAffectedEndIndex;
        int timeShiftOfAffectedCommercials;
        int timeShiftOfShiftedCommercial;
        if (n1 < n2) {
            calculateRevenueAffectedStartIndex = n1 + 1;
            calculateRevenueAffectedEndIndex = n2;
            timeShiftOfAffectedCommercials = -n1Commercial.getDuration();
            timeShiftOfShiftedCommercial = n2SolutionData.getStartTime() + n2Commercial.getDuration() - n1Commercial.getDuration() - n1SolutionData.getStartTime();
        } else {
            calculateRevenueAffectedStartIndex = n2;
            calculateRevenueAffectedEndIndex = n1 - 1;
            timeShiftOfAffectedCommercials = n1Commercial.getDuration();
            timeShiftOfShiftedCommercial = n2SolutionData.getStartTime() - n1SolutionData.getStartTime();
        }

        var solutionDataList = solution.solution.get(inventory.getId());

        double gain = 0;

        for (var i = calculateRevenueAffectedStartIndex; i <= calculateRevenueAffectedEndIndex; i++) {
            var solutionData = solutionDataList.get(i);
            gain += MoveUtils.calculateRevenueChange(solutionData, inventory, timeShiftOfAffectedCommercials);
        }

        gain += MoveUtils.calculateRevenueChange(
                n1SolutionData,
                inventory,
                timeShiftOfShiftedCommercial
        );

        this.revenueGain = gain;
        this.isRevenueGainCalculated = true;
        return this.revenueGain;
    }

    @Override
    public boolean checkFeasibility() {
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

    private boolean conditionalCheck(){
        var inventoryList = solution.solution.get(inventory.getId());
        var solutionDataN1 = inventoryList.get(n1);
        var solutionDataN2 = inventoryList.get(n2);

        if (n1 < n2){
            if (!MoveUtils.isAttentionSatisfied(
                    solutionDataN1.getCommercial(),
                    inventory,
                    n2,
                    solutionDataN2.getStartTime() + solutionDataN2.getCommercial().getDuration() - solutionDataN1.getCommercial().getDuration(),
                    inventoryList.size())) return false;

            if (!MoveUtils.isGroupConstraintsSatisfied(
                    n2Commercial,
                    n1Commercial,
                    n2 + 1 <= lastIndexOfInventory ? inventoryList.get(n2 + 1).getCommercial() : null)) return false;
        }

        if (n1 >= n2){
            for (var i = n2; i < n1; i++) {
                var solutionData = inventoryList.get(i);
                if (!MoveUtils.isAttentionSatisfied(
                        solutionData.getCommercial(),
                        inventory,
                        i + 1,
                        solutionData.getStartTime() + solutionDataN1.getCommercial().getDuration(),
                        inventoryList.size())) return false;
            }

            if (!MoveUtils.isAttentionSatisfied(
                    solutionDataN1.getCommercial(),
                    inventory,
                    n2,
                    solutionDataN2.getStartTime(),
                    inventoryList.size())) return false;

            if (!MoveUtils.isGroupConstraintsSatisfied(
                    n2 - 1 >= 0 ? inventoryList.get(n2 - 1).getCommercial() : null,
                    solutionDataN1.getCommercial(),
                    solutionDataN2.getCommercial())) return false;
        }

        if (n2 == lastIndexOfInventory){
            if (inventoryList.get(lastIndexOfInventory).getCommercial().getAttentionMapArray()[inventory.getId()] == ATTENTION.LAST) return false;
        }

        return true;
    }

    private boolean generalCheck() {
        var inventoryList = solution.solution.get(inventory.getId());
        if (!MoveUtils.isGroupConstraintsSatisfied(
                n1 - 1 >= 0 ? inventoryList.get(n1 - 1).getCommercial() : null,
                n1 + 1 < inventoryList.size() ? inventoryList.get(n1 + 1).getCommercial() : null)) return false;

        return true;
    }

    public Solution getSolution() {
        return solution;
    }
}
