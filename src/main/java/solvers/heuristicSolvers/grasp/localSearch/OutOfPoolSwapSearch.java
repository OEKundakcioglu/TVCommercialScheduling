package solvers.heuristicSolvers.grasp.localSearch;

import data.Solution;
import solvers.heuristicSolvers.grasp.localSearch.move.OutOfPoolSwapMove;
import data.ProblemParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class OutOfPoolSwapSearch extends BaseSearch {
    private final double[] totalCommercialDurationOfHour;

    public OutOfPoolSwapSearch(Solution currentSolution, ProblemParameters parameters, boolean getAllNeighborhood, boolean isBestMove, Random random) throws Exception {
        super(currentSolution, parameters, getAllNeighborhood, isBestMove, random);
        this.totalCommercialDurationOfHour = new double[parameters.getSetOfHours().size() + 1];
        for (var solutionDataList : currentSolution.solution) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();
            var hour = inventory.getHour();
            for (var solutionData : solutionDataList) {
                totalCommercialDurationOfHour[hour] += solutionData.getCommercial().getDuration();
            }
        }

        if (!isBestMove) this.firstImprovingSearch();
        else this.bestImprovingSearch();
    }

    private void firstImprovingSearch() throws Exception {
        this.bestFoundSolution = currentSolution;
        var unassignedCommercials = new ArrayList<>(this.parameters.getSetOfCommercials());
        for (var solutionDataList : currentSolution.solution) {
            for (var solutionData : solutionDataList) {
                unassignedCommercials.remove(solutionData.getCommercial());
            }
        }

        Collections.shuffle(unassignedCommercials, random);

        for (var commercial : unassignedCommercials) {
            for (var inventory : super.getShuffledList(commercial.getSetOfSuitableInv())) {
                var solutionDataList = currentSolution.solution.get(inventory.getId());
                for (var position : getShuffledIndexList(0, solutionDataList.size())) {

                    var outOfLoopSwapMove = new OutOfPoolSwapMove(currentSolution, position, commercial, inventory, this.totalCommercialDurationOfHour);

                    if (!outOfLoopSwapMove.checkFeasibility()) continue;

                    var doStop = !super.update(outOfLoopSwapMove);
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

        Collections.shuffle(unassignedCommercials, random);

        for (var commercial : unassignedCommercials) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                var solutionDataList = currentSolution.solution.get(inventory.getId());
                for (var position = 0; position < solutionDataList.size(); position++) {

                    var outOfLoopSwapMove = new OutOfPoolSwapMove(currentSolution, position, commercial, inventory, this.totalCommercialDurationOfHour);

                    if (!outOfLoopSwapMove.checkFeasibility()) continue;

                    var doStop = !super.update(outOfLoopSwapMove);
                    if (doStop) return;
                }
            }
        }
    }

}
