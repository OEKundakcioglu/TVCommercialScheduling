package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;
import solvers.heuristicSolvers.grasp.localSearch.move.ChainSwapMove;

import java.util.Random;

/**
 * Search for 3-way chain swap moves.
 * c1 -> i2, c2 -> i3, c3 -> i1 (cyclic rotation)
 * Useful for escaping local optima that 2-swap cannot escape.
 * Has O(S^3) neighborhood so should be used sparingly (e.g., during stagnation).
 */
public class ChainSwapSearch extends BaseSearch {
    private final double[] totalCommercialDurationOfHour;

    public ChainSwapSearch(
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
        var allSolutionData = super.currentSolution.getSortedSolutionData();
        var randomIndices = super.getShuffledIndexList(0, allSolutionData.length);

        // O(N^3) search - expensive, so we use first improvement with randomization
        for (int idx1 = 0; idx1 < randomIndices.size() - 2; idx1++) {
            var s1 = allSolutionData[randomIndices.get(idx1)];
            if (s1 == null) continue;

            for (int idx2 = idx1 + 1; idx2 < randomIndices.size() - 1; idx2++) {
                var s2 = allSolutionData[randomIndices.get(idx2)];
                if (s2 == null) continue;
                if (s2.getInventory() == s1.getInventory()) continue;

                for (int idx3 = idx2 + 1; idx3 < randomIndices.size(); idx3++) {
                    var s3 = allSolutionData[randomIndices.get(idx3)];
                    if (s3 == null) continue;
                    if (s3.getInventory() == s1.getInventory()) continue;
                    if (s3.getInventory() == s2.getInventory()) continue;

                    // All three must be from different inventories
                    var i1 = s1.getInventory();
                    var i2 = s2.getInventory();
                    var i3 = s3.getInventory();

                    // Check suitability before creating move
                    var c1 = s1.getCommercial();
                    var c2 = s2.getCommercial();
                    var c3 = s3.getCommercial();

                    // c3 -> i1, c1 -> i2, c2 -> i3
                    if (!c3.isInventorySuitable(i1)) continue;
                    if (!c1.isInventorySuitable(i2)) continue;
                    if (!c2.isInventorySuitable(i3)) continue;

                    var move = new ChainSwapMove(
                            super.currentSolution,
                            i1, s1.getPosition(),
                            i2, s2.getPosition(),
                            i3, s3.getPosition(),
                            totalCommercialDurationOfHour);

                    if (!move.checkFeasibility()) continue;

                    var doStop = !super.update(move);
                    if (doStop) return;
                }
            }
        }
    }

    private void bestImprovingSearch() throws Exception {
        var allSolutionData = super.currentSolution.getSortedSolutionData();

        // Full enumeration for best improvement - very expensive!
        for (int idx1 = 0; idx1 < allSolutionData.length - 2; idx1++) {
            var s1 = allSolutionData[idx1];
            if (s1 == null) continue;

            for (int idx2 = idx1 + 1; idx2 < allSolutionData.length - 1; idx2++) {
                var s2 = allSolutionData[idx2];
                if (s2 == null) continue;
                if (s2.getInventory() == s1.getInventory()) continue;

                for (int idx3 = idx2 + 1; idx3 < allSolutionData.length; idx3++) {
                    var s3 = allSolutionData[idx3];
                    if (s3 == null) continue;
                    if (s3.getInventory() == s1.getInventory()) continue;
                    if (s3.getInventory() == s2.getInventory()) continue;

                    var i1 = s1.getInventory();
                    var i2 = s2.getInventory();
                    var i3 = s3.getInventory();

                    var c1 = s1.getCommercial();
                    var c2 = s2.getCommercial();
                    var c3 = s3.getCommercial();

                    if (!c3.isInventorySuitable(i1)) continue;
                    if (!c1.isInventorySuitable(i2)) continue;
                    if (!c2.isInventorySuitable(i3)) continue;

                    var move = new ChainSwapMove(
                            super.currentSolution,
                            i1, s1.getPosition(),
                            i2, s2.getPosition(),
                            i3, s3.getPosition(),
                            totalCommercialDurationOfHour);

                    if (!move.checkFeasibility()) continue;

                    var doStop = !super.update(move);
                    if (doStop) return;
                }
            }
        }
    }
}
