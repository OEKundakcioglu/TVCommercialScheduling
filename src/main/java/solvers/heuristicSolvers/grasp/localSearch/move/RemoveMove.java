package solvers.heuristicSolvers.grasp.localSearch.move;

import data.*;
import runParameters.LoopSetup;

import java.util.List;


public class RemoveMove implements IMove{

    private final Solution solution;
    private final Inventory i;
    private final Commercial c;
    private final SolutionData cSolutionData;
    private final List<SolutionData> solutionDataList;
    private final int n;
    private final int lastIndexOfInventory;

    private double revenueGain;
    private boolean isRevenueGainCalculated = false;
    private boolean isFeasible;
    private boolean isFeasibilityChecked = false;

    public RemoveMove(Solution solution, Inventory i, int n) {
        this.solution = solution;
        this.i = i;
        this.n = n;

        this.solutionDataList = solution.solution.get(i.getId());
        this.cSolutionData = solutionDataList.get(n);
        this.c = cSolutionData.getCommercial();
        this.lastIndexOfInventory = solutionDataList.size() - 1;
    }

    public Solution applyMove() throws Exception {
        var newSolution = solution.copy();
        var solutionDataList = newSolution.solution.get(i.getId());
        var solutionData = newSolution.getSolutionData(c);

        newSolution.removeSolutionData(i, solutionData);

        for (var l = n; l <= lastIndexOfInventory-1; l++){
            solutionData = solutionDataList.get(l);
            MoveUtils.updateSolutionData(
                    solutionData,
                    i,
                    - c.getDuration(),
                    l
            );
        }

        newSolution.revenue += calculateRevenueGain();

        if (LoopSetup.isDebug) Utils.feasibilityCheck(newSolution);

        return newSolution;
    }

    public double calculateRevenueGain(){
        if (isRevenueGainCalculated) return revenueGain;

        double gain = 0;
        for (var l = n + 1; l <= lastIndexOfInventory; l++){
            var solutionData = solutionDataList.get(l);
            gain += MoveUtils.calculateRevenueChange(
                    solutionData,
                    i,
                    - c.getDuration()
            );
        }

        gain -= cSolutionData.getRevenue();

        revenueGain = gain;
        isRevenueGainCalculated = true;

        return revenueGain;
    }

    public boolean checkFeasibility(){
        if (isFeasibilityChecked) return isFeasible;

        if (!MoveUtils.isGroupConstraintsSatisfied(
                n > 0 ? solutionDataList.get(n-1).getCommercial() : null,
                n < lastIndexOfInventory ? solutionDataList.get(n+1).getCommercial() : null
        )) {
            isFeasible = false;
            return false;
        }

        isFeasibilityChecked = true;
        isFeasible = true;
        return true;
    }

    public Solution getSolution() {
        return solution;
    }
}
