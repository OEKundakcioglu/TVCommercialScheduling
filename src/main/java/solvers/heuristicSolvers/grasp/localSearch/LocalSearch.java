package solvers.heuristicSolvers.grasp.localSearch;

import data.Solution;
import data.ProblemParameters;
import runParameters.LocalSearchSettings;

import java.util.List;
import java.util.Random;

public class LocalSearch {
    private Solution bestFoundSolution;
    private final Solution currentSolution;
    private final ProblemParameters parameters;
    private final LocalSearchSettings localSearchSettings;
    private transient final Random random;

    public LocalSearch(Solution currentSolution, ProblemParameters parameters, boolean isBestMove, LocalSearchSettings localSearchSettings, Random random) throws Exception {
        this.currentSolution = currentSolution;
        this.bestFoundSolution = currentSolution;
        this.parameters = parameters;
        this.localSearchSettings = localSearchSettings;
        this.random = random;
        this.search(isBestMove);
    }

    private void search(boolean isBestMove) throws Exception {
        int k = 0;
        this.bestFoundSolution = currentSolution;

        var solution = currentSolution;
        while (k < localSearchSettings.moves.size()){
            var moveString = localSearchSettings.moves.get(k);
            solution = applySearch(moveString, solution, isBestMove);
            if (solution.revenue > bestFoundSolution.revenue){
                k = 0;
                bestFoundSolution = solution;
            }
            else {
//                for (var movestr : List.of("outOfPool", "insert")) {
//                    var tempSolution = applySearch(movestr, solution, isBestMove);
//                    if (tempSolution.revenue > solution.revenue) {
//                        solution = tempSolution;
//                    }
//                }
                k++;
            }
        }
    }

    @SuppressWarnings("IfCanBeSwitch")
    public Solution applySearch(String key, Solution solution, boolean isBestMove) throws Exception {
        BaseSearch search;

        if (key.equals("insert")) search = new InsertSearch(solution, parameters, false, isBestMove, random);
        else if (key.equals("transfer")) search = new TransferSearch(solution, parameters, false, isBestMove, random);
        else if (key.equals("outOfPool")) search = new OutOfPoolSwapSearch(solution, parameters, false, isBestMove, random);
        else if (key.equals("intraSwap")) search = new IntraSwapSearch(solution, parameters, false, isBestMove, random);
        else if (key.equals("interSwap")) search = new InterSwapSearch(solution, parameters, false, isBestMove, random);
        else if (key.equals("shift")) search = new ShiftSearch(solution, parameters, false, isBestMove, random);
        else throw new RuntimeException("Invalid key");

        return search.getSolution();
    }

    public Solution getSolution() {
        return bestFoundSolution;
    }
}
