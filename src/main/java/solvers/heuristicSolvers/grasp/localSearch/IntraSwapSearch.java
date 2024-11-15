package solvers.heuristicSolvers.grasp.localSearch;

import data.Solution;
import solvers.heuristicSolvers.grasp.localSearch.move.IntraSwapMove;
import data.ProblemParameters;

import java.util.Random;

public class IntraSwapSearch extends BaseSearch {

    public IntraSwapSearch(Solution currentSolution, ProblemParameters parameters, boolean getAllNeighborhood, SearchMode searchMode, Random random) throws Exception {
        super(currentSolution, parameters, getAllNeighborhood, searchMode, random);

        if (searchMode == SearchMode.FIRST_IMPROVEMENT) this.firstImprovingSearch();
        else if(searchMode == SearchMode.BEST_IMPROVEMENT) this.bestImprovingSearch();
    }

    private void firstImprovingSearch() throws Exception {
        this.bestFoundSolution = currentSolution;

        for (var solutionDataList : super.getShuffledList(currentSolution.solution)) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();

            for (var i : super.getShuffledIndexList(0, solutionDataList.size() - 1)){
                for (var j : super.getShuffledIndexList(i + 1, solutionDataList.size())){
                    var swapMove = new IntraSwapMove(currentSolution, inventory, i, j);

                    if (!swapMove.checkFeasibility()) continue;

                    var doStop = !super.update(swapMove);
                    if (doStop) return;
                }
            }
        }
    }

    private void bestImprovingSearch() throws Exception {
        this.bestFoundSolution = currentSolution;

        for (var solutionDataList : currentSolution.solution) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();

            for (var i = 0; i < solutionDataList.size() - 1; i++){
                for (var j = i+1; j < solutionDataList.size(); j++ ){
                    var swapMove = new IntraSwapMove(currentSolution, inventory, i, j);

                    if (!swapMove.checkFeasibility()) continue;

                    var doStop = !super.update(swapMove);
                    if (doStop) return;
                }
            }
        }
    }
}
