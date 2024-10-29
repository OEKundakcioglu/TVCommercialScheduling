package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import runParameters.LoopSetup;

import java.util.Collections;
import java.util.List;

public class IntraSwapMove implements IMove {

    private final Solution solution;
    private final Inventory inventory;
    private final int n1;
    private final int n2;

    private final Commercial n1Commercial;
    private final Commercial n2Commercial;
    private final SolutionData n1SolutionData;
    private final SolutionData n2SolutionData;
    private final double shift;
    private final int lastIndexOfInventory;
    private final List<SolutionData> inventoryList;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;

    public IntraSwapMove(Solution solution, Inventory inventory, int n1, int n2) {
        assert n1 < n2;

        this.solution = solution;
        this.inventory = inventory;
        this.n1 = n1;
        this.n2 = n2;

        inventoryList = solution.solution.get(inventory.getId());
        n1SolutionData = inventoryList.get(n1);
        n2SolutionData = inventoryList.get(n2);
        n1Commercial = n1SolutionData.getCommercial();
        n2Commercial = n2SolutionData.getCommercial();
        lastIndexOfInventory = inventoryList.size() - 1;


        this.shift = n2Commercial.getDuration() - n1Commercial.getDuration();
    }

    public Solution applyMove() throws Exception {
        // Swap the elements at indices i and j
        var newSolution = solution.copy();
        var newSolutionDataList = newSolution.solution.get(inventory.getId());

        Collections.swap(newSolutionDataList, n1, n2);

        for (var i = n1 + 1; i <= n2 - 1; i++) {
            var solutionData = newSolutionDataList.get(i);
            MoveUtils.updateSolutionData(
                    solutionData,
                    inventory,
                    shift,
                    i
            );
        }

        var newN1SolutionData = newSolution.getSolutionData(n1Commercial);
        var n1Shift = n2SolutionData.getStartTime() + shift - newN1SolutionData.getStartTime();
        MoveUtils.updateSolutionData(
                newN1SolutionData,
                inventory,
                n1Shift,
                n2
        );

        var newN2SolutionData = newSolution.getSolutionData(n2Commercial);
        var n2Shift = n1SolutionData.getStartTime() - newN2SolutionData.getStartTime();
        MoveUtils.updateSolutionData(
                newN2SolutionData,
                inventory,
                n2Shift,
                n1
        );

        newSolution.revenue += calculateRevenueGain();

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }


    public double calculateRevenueGain() {
        if (isRevenueGainCalculated) return revenueGain;

        var solutionDataList = solution.solution.get(inventory.getId());

        double gain = 0;
        for (var i = n1 + 1; i <= n2 - 1; i++) {
            var solutionData = solutionDataList.get(i);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    inventory,
                    shift
            );
        }

        gain += MoveUtils.calculateRevenueChange(
                n1SolutionData,
                inventory,
                n2SolutionData.getStartTime() + shift - n1SolutionData.getStartTime()
        );

        gain += MoveUtils.calculateRevenueChange(
                n2SolutionData,
                inventory,
                n1SolutionData.getStartTime() - n2SolutionData.getStartTime()
        );

        this.revenueGain = gain;
        this.isRevenueGainCalculated = true;

        return revenueGain;
    }

    public boolean checkFeasibility() {
        if (isFeasibilityChecked) return isFeasible;

        isFeasibilityChecked = true;

        if (!generalCheck()) {
            isFeasible = false;
            return false;
        }

        if (!conditionalCheck()) {
            isFeasible = false;
            return false;
        }

        isFeasible = true;
        return true;
    }


    @SuppressWarnings("RedundantIfStatement")
    private boolean generalCheck() {
        if (!MoveUtils.isAttentionSatisfied(
                n1Commercial,
                inventory,
                n2,
                n2SolutionData.getStartTime() + shift,
                this.lastIndexOfInventory)) return false;

        if (!MoveUtils.isAttentionSatisfied(
                n2Commercial,
                inventory,
                n1,
                n1SolutionData.getStartTime(),
                this.lastIndexOfInventory)) return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n1 - 1 >= 0 ? inventoryList.get(n1 - 1).getCommercial() : null,
                n2Commercial,
                inventoryList.get(n1 + 1).getCommercial())) return false;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                inventoryList.get(n2 - 1).getCommercial(),
                n1Commercial,
                n2 + 1 <= lastIndexOfInventory ? inventoryList.get(n2 + 1).getCommercial() : null)) return false;

        return true;
    }

    private boolean conditionalCheck() {
        if (n1Commercial.getDuration() < n2Commercial.getDuration()) {
            for (var i = n1 + 1; i <= n2 - 1; i++) {
                var solutionData = inventoryList.get(i);
                if (!MoveUtils.isAttentionSatisfied(
                        solutionData.getCommercial(),
                        inventory,
                        i,
                        solutionData.getStartTime() + shift,
                        this.lastIndexOfInventory)) return false;
            }
        }

        return true;
    }

    public Solution getSolution() {
        return solution;
    }
}
