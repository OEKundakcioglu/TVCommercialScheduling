package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;
import solvers.heuristicSolvers.grasp.localSearch.move.TransferMove;

import java.util.Random;

public class TransferSearch extends BaseSearch {
    private final double[] totalCommercialDurationOfHour;

    public TransferSearch(
            Solution currentSolution,
            ProblemParameters parameters,
            boolean getAllNeighborhood,
            SearchMode searchMode,
            Random random)
            throws Exception {
        super(currentSolution, parameters, getAllNeighborhood, searchMode, random);
        this.totalCommercialDurationOfHour = new double[parameters.getSetOfHours().size() + 1];
        for (var solutionDataList : currentSolution.solution) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();
            var hour = inventory.getHour();
            for (var solutionData : solutionDataList) {
                totalCommercialDurationOfHour[hour] += solutionData.getCommercial().getDuration();
            }
        }

        if (searchMode == SearchMode.FIRST_IMPROVEMENT) this.firstImprovingSearch();
        else if (searchMode == SearchMode.BEST_IMPROVEMENT) this.bestImprovingSearch();
    }

    private void firstImprovingSearch() throws Exception {
        this.bestFoundSolution = currentSolution;

        for (var solutionDataList : getShuffledList(currentSolution.solution)) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();
            for (var n1 : getShuffledIndexList(0, solutionDataList.size())) {
                var solutionData = solutionDataList.get(n1);
                var commercial = solutionData.getCommercial();
                for (var inventory2 : getShuffledList(commercial.getSetOfSuitableInv())) {
                    if (inventory == inventory2) continue;

                    var solutionDataList2 = currentSolution.solution.get(inventory2.getId());
                    for (var n2 : getShuffledIndexList(0, solutionDataList2.size() + 1)) {
                        var transferMove =
                                new TransferMove(
                                        currentSolution,
                                        inventory,
                                        inventory2,
                                        n1,
                                        n2,
                                        this.totalCommercialDurationOfHour);

                        if (!transferMove.checkFeasibility()) continue;

                        var doStop = !super.update(transferMove);
                        if (doStop) return;
                    }
                }
            }
        }
    }

    private void bestImprovingSearch() throws Exception {
        this.bestFoundSolution = currentSolution;

        for (var solutionDataList : currentSolution.solution) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();
            for (var n1 = 0; n1 < solutionDataList.size(); n1++) {
                var solutionData = solutionDataList.get(n1);
                var commercial = solutionData.getCommercial();
                for (var inventory2 : commercial.getSetOfSuitableInv()) {
                    if (inventory == inventory2) continue;

                    var solutionDataList2 = currentSolution.solution.get(inventory2.getId());
                    for (var n2 = 0; n2 < solutionDataList2.size() + 1; n2++) {
                        var transferMove =
                                new TransferMove(
                                        currentSolution,
                                        inventory,
                                        inventory2,
                                        n1,
                                        n2,
                                        this.totalCommercialDurationOfHour);

                        if (!transferMove.checkFeasibility()) continue;

                        var doStop = !super.update(transferMove);
                        if (doStop) return;
                    }
                }
            }
        }
    }
}
