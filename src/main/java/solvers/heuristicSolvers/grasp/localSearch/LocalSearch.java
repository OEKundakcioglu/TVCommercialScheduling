package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;
import runParameters.LocalSearchSettings;
import solvers.GlobalRandom;

public class LocalSearch {
    private Solution bestFoundSolution;
    private final Solution currentSolution;
    private final ProblemParameters parameters;
    private final LocalSearchSettings localSearchSettings;

    public LocalSearch(Solution currentSolution, ProblemParameters parameters, SearchMode searchMode, LocalSearchSettings localSearchSettings) throws Exception {
        this.currentSolution = currentSolution;
        this.bestFoundSolution = currentSolution;
        this.parameters = parameters;
        this.localSearchSettings = localSearchSettings;
        this.search(searchMode);
    }

    private void search(SearchMode searchMode) throws Exception {
        int k = 0;
        this.bestFoundSolution = currentSolution;

        var solution = currentSolution;
        while (k < localSearchSettings.moves.size()){
            if (GlobalRandom.getRandom().nextDouble() < localSearchSettings.neighborhoodSkipProbability) {
                k++;
                continue;
            }

            var moveString = localSearchSettings.moves.get(k);
            solution = applySearch(moveString, solution, searchMode);
            if (solution.revenue > bestFoundSolution.revenue){
                k = 0;
                bestFoundSolution = solution;
            }
            else {
                k++;
            }
        }
    }

    @SuppressWarnings("IfCanBeSwitch")
    public Solution applySearch(String key, Solution solution, SearchMode searchMode) throws Exception {
        BaseSearch search;

        if (key.equals("insert")) search = new InsertSearch(solution, parameters, false, searchMode);
        else if (key.equals("transfer")) search = new TransferSearch(solution, parameters, false, searchMode);
        else if (key.equals("outOfPool")) search = new OutOfPoolSwapSearch(solution, parameters, false, searchMode);
        else if (key.equals("intraSwap")) search = new IntraSwapSearch(solution, parameters, false, searchMode);
        else if (key.equals("interSwap")) search = new InterSwapSearch(solution, parameters, false, searchMode);
        else if (key.equals("shift")) search = new ShiftSearch(solution, parameters, false, searchMode);
        else throw new RuntimeException("Invalid key");

        return search.getSolution();
    }

    public Solution getSolution() {
        return bestFoundSolution;
    }
}
