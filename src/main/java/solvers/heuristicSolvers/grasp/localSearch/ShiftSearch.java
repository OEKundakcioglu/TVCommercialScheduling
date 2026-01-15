package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;

import solvers.heuristicSolvers.grasp.localSearch.move.ShiftMove;

import java.util.Random;

public class ShiftSearch extends BaseSearch {

    public ShiftSearch(
            Solution currentSolution,
            ProblemParameters parameters,
            boolean getAllNeighborhood,
            SearchMode searchMode,
            Random random)
            throws Exception {
        super(currentSolution, parameters, getAllNeighborhood, searchMode, random);

        if (searchMode == SearchMode.FIRST_IMPROVEMENT) this.firstImprovingSearch();
        else if (searchMode == SearchMode.BEST_IMPROVEMENT) this.bestImprovingSearch();
    }

    private void firstImprovingSearch() throws Exception {
        for (var solutionDataList : getShuffledList(currentSolution.solution)) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();

            for (var solutionData : getShuffledList(solutionDataList)) {
                var fromIndex = solutionDataList.indexOf(solutionData);
                for (var toIndex : getShuffledIndexList(0, solutionDataList.size())) {
                    if (fromIndex == toIndex) continue;
                    var shiftMove = new ShiftMove(currentSolution, inventory, toIndex, fromIndex);
                    if (!shiftMove.checkFeasibility()) continue;

                    var doStop = !super.update(shiftMove);
                    if (doStop) return;
                }
            }
        }
    }

    private void bestImprovingSearch() throws Exception {
        for (var solutionDataList : currentSolution.solution) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();

            for (var fromIndex = 0; fromIndex < solutionDataList.size(); fromIndex++) {
                for (var toIndex = 0; toIndex < solutionDataList.size(); toIndex++) {
                    if (fromIndex == toIndex) continue;
                    var shiftMove = new ShiftMove(currentSolution, inventory, toIndex, fromIndex);
                    if (!shiftMove.checkFeasibility()) continue;

                    var doStop = !super.update(shiftMove);
                    if (doStop) return;
                }
            }
        }
    }

}
