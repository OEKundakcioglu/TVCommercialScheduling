package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;
import solvers.heuristicSolvers.grasp.localSearch.move.InsertMove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@SuppressWarnings("DuplicatedCode")
public class InsertSearch extends BaseSearch {
    private final double[] totalCommercialDurationOfHour;

    public InsertSearch(
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
        var unassignedCommercials = new ArrayList<>(this.parameters.getSetOfCommercials());
        for (var solutionDataList : currentSolution.solution) {
            for (var solutionData : solutionDataList) {
                unassignedCommercials.remove(solutionData.getCommercial());
            }
        }

        Collections.shuffle(unassignedCommercials, this.random);

        for (var commercial : unassignedCommercials) {
            for (var inventory : super.getShuffledList(commercial.getSetOfSuitableInv())) {
                var solutionDataList = currentSolution.solution.get(inventory.getId());
//                if (solutionDataList.isEmpty()) continue;
                for (var i : super.getShuffledIndexList(0, solutionDataList.size() + 1)) {
                    var insertMove =
                            new InsertMove(
                                    currentSolution,
                                    commercial,
                                    inventory,
                                    i,
                                    this.totalCommercialDurationOfHour);

                    if (!insertMove.checkFeasibility()) continue;

                    var doStop = !super.update(insertMove);
                    if (doStop) return;
                }
            }
        }
    }

    private void bestImprovingSearch() throws Exception {
        this.bestFoundSolution = currentSolution;
        var unassignedCommercials = new ArrayList<>(this.parameters.getSetOfCommercials());
        for (var solutionDataList : currentSolution.solution) {
            for (var solutionData : solutionDataList) {
                unassignedCommercials.remove(solutionData.getCommercial());
            }
        }

        Collections.shuffle(unassignedCommercials, this.random);

        for (var commercial : unassignedCommercials) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                var solutionDataList = currentSolution.solution.get(inventory.getId());
//                if (solutionDataList.isEmpty()) continue;
                for (var i = 0; i <= solutionDataList.size(); i++) {
                    var insertMove =
                            new InsertMove(
                                    currentSolution,
                                    commercial,
                                    inventory,
                                    i,
                                    this.totalCommercialDurationOfHour);

                    if (!insertMove.checkFeasibility()) continue;

                    var doStop = !super.update(insertMove);
                    if (doStop) return;
                }
            }
        }
    }
}
