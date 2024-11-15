package solvers.heuristicSolvers.grasp.localSearch;

import data.Commercial;
import data.Inventory;
import data.ProblemParameters;
import data.Solution;

import solvers.heuristicSolvers.grasp.localSearch.move.InterSwapMove;

import java.util.Random;

public class InterSwapSearch extends BaseSearch{
    private final double[] totalCommercialDurationOfHour;

    InterSwapSearch(Solution currentSolution, ProblemParameters parameters, boolean getAllNeighborhood, SearchMode searchMode, Random random) throws Exception {
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
        else if(searchMode == SearchMode.BEST_IMPROVEMENT) this.bestImprovingSearch();
    }

    private void firstImprovingSearch() throws Exception {
        var allSolutionData = super.currentSolution.getSortedSolutionData();
        var randomIndices = super.getShuffledIndexList(0, allSolutionData.length);

        for (var s1Index = 0; s1Index < randomIndices.size()-1; s1Index++){
            var s1 = allSolutionData[s1Index];
            if (s1 == null) continue;

            for (var s2Index = s1Index + 1; s2Index < randomIndices.size()-1; s2Index++){
                var s2 = allSolutionData[s2Index];
                if (s2 == null) continue;

                if (s1.getInventory() == s2.getInventory()) continue;

                Inventory i1;
                Inventory i2;
                Commercial c1;
                Commercial c2;
                int n1;
                int n2;

                if (s1.getCommercial().getDuration() <= s2.getCommercial().getDuration()){
                    i1 = s1.getInventory();
                    i2 = s2.getInventory();
                    n1 = s1.getPosition();
                    n2 = s2.getPosition();
                    c1 = s1.getCommercial();
                    c2 = s2.getCommercial();
                } else {
                    i1 = s2.getInventory();
                    i2 = s1.getInventory();
                    n1 = s2.getPosition();
                    n2 = s1.getPosition();
                    c1 = s2.getCommercial();
                    c2 = s1.getCommercial();
                }

                if (!c1.isInventorySuitable(i2)) continue;
                if (!c2.isInventorySuitable(i1)) continue;

                var move = new InterSwapMove(super.currentSolution, i1, n1, i2, n2, totalCommercialDurationOfHour);

                if (!move.checkFeasibility()) continue;

                var doStop = !super.update(move);
                if (doStop) return;
            }
        }
    }

    private void bestImprovingSearch() throws Exception {
        var allSolutionData = super.currentSolution.getSortedSolutionData();

        for (var s1Index = 0; s1Index < allSolutionData.length-1; s1Index++){
            var s1 = allSolutionData[s1Index];
            if (s1 == null) continue;

            for (var s2Index = s1Index + 1; s2Index < allSolutionData.length-1; s2Index++){
                var s2 = allSolutionData[s2Index];
                if (s2 == null) continue;

                if (s1.getInventory() == s2.getInventory()) continue;

                Inventory i1;
                Inventory i2;
                Commercial c1;
                Commercial c2;
                int n1;
                int n2;

                if (s1.getCommercial().getDuration() <= s2.getCommercial().getDuration()){
                    i1 = s1.getInventory();
                    i2 = s2.getInventory();
                    n1 = s1.getPosition();
                    n2 = s2.getPosition();
                    c1 = s1.getCommercial();
                    c2 = s2.getCommercial();
                } else {
                    i1 = s2.getInventory();
                    i2 = s1.getInventory();
                    n1 = s2.getPosition();
                    n2 = s1.getPosition();
                    c1 = s2.getCommercial();
                    c2 = s1.getCommercial();
                }

                if (!c1.isInventorySuitable(i2)) continue;
                if (!c2.isInventorySuitable(i1)) continue;

                var move = new InterSwapMove(super.currentSolution, i1, n1, i2, n2, totalCommercialDurationOfHour);

                if (!move.checkFeasibility()) continue;

                var doStop = !super.update(move);
                if (doStop) return;
            }
        }
    }
}
