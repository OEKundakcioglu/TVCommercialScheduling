package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;

import solvers.heuristicSolvers.grasp.localSearch.move.IMove;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class BaseSearch {
    protected final List<IMove> neighborhood;
    protected Solution currentSolution;
    protected Solution bestFoundSolution;
    protected ProblemParameters parameters;
    protected boolean getAllNeighborhood;
    protected SearchMode searchMode;

    public static int moveCount = 0;

    protected Random random;
    protected IMove bestMove;
    protected double bestRevenue;

    protected boolean bestMoveUsed = false;

    BaseSearch(
            Solution currentSolution,
            ProblemParameters parameters,
            boolean getAllNeighborhood,
            SearchMode searchMode,
            Random random) {

        this.currentSolution = currentSolution;
        this.parameters = parameters;
        this.getAllNeighborhood = getAllNeighborhood;
        this.bestFoundSolution = currentSolution;
        this.searchMode = searchMode;
        this.neighborhood = new ArrayList<>();
        this.random = random;
        this.bestRevenue = currentSolution.revenue;
    }

    protected boolean update(IMove move) throws Exception {
        moveCount++;
        var revenueGain = move.calculateRevenueGain();

        if (searchMode == SearchMode.RANDOM) {
            bestFoundSolution = move.applyMove();
            return false;
        }

        if (revenueGain + currentSolution.revenue > bestRevenue) {
            bestRevenue = revenueGain + currentSolution.revenue;
            bestMove = move;

            if (searchMode == SearchMode.FIRST_IMPROVEMENT) {
                return false;
            }
        }

        return true;
    }

    public Solution getSolution() throws Exception {
        if (bestMove == null) return currentSolution;
        if (bestMoveUsed) return bestFoundSolution;
        bestMoveUsed = true;
        bestFoundSolution = bestMove.applyMove();
        return bestFoundSolution;
    }

    protected <T> List<T> getShuffledList(Collection<T> list) {
        var shuffledList = new ArrayList<>(list);
        Collections.shuffle(shuffledList, this.random);
        return shuffledList;
    }

    protected List<Integer> getShuffledIndexList(int start, int excludedEnd) {
        var shuffledList = new ArrayList<Integer>();
        for (var i = start; i < excludedEnd; i++) {
            shuffledList.add(i);
        }
        Collections.shuffle(shuffledList, this.random);
        return shuffledList;
    }
}
