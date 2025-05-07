package solvers.heuristicSolvers.grasp.localSearch;

import data.ProblemParameters;
import data.Solution;

import solvers.heuristicSolvers.grasp.localSearch.move.IMove;

import java.util.*;

public abstract class BaseSearch {
    protected final List<IMove> neighborhood;
    protected Solution currentSolution;
    protected Solution bestFoundSolution;
    protected ProblemParameters parameters;
    protected boolean getAllNeighborhood;
    protected SearchMode searchMode;
    protected Random random;

    public static int moveCount = 0;

    BaseSearch(
            Solution currentSolution,
            ProblemParameters parameters,
            boolean getAllNeighborhood,
            SearchMode searchMode,
            Random random) {
        this.random = random;

        this.currentSolution = currentSolution;
        this.parameters = parameters;
        this.getAllNeighborhood = getAllNeighborhood;
        this.bestFoundSolution = currentSolution;
        this.searchMode = searchMode;
        this.neighborhood = new ArrayList<>();
    }

    protected boolean update(IMove move) throws Exception {
        moveCount++;
        var revenueGain = move.calculateRevenueGain();

        if (searchMode == SearchMode.RANDOM) {
            bestFoundSolution = move.applyMove();
            return false;
        }

        if (revenueGain + currentSolution.revenue > bestFoundSolution.revenue) {
            bestFoundSolution = move.applyMove();

            return searchMode != SearchMode.FIRST_IMPROVEMENT;
        }

        return true;
    }

    public Solution getSolution() {
        return bestFoundSolution != null ? bestFoundSolution : currentSolution;
    }

    protected <T> List<T> getShuffledList(Collection<T> list) {
        var shuffledList = new ArrayList<>(list);
        Collections.shuffle(shuffledList, random);
        return shuffledList;
    }

    protected List<Integer> getShuffledIndexList(int start, int excludedEnd) {
        var shuffledList = new ArrayList<Integer>();
        for (var i = start; i < excludedEnd; i++) {
            shuffledList.add(i);
        }
        Collections.shuffle(shuffledList, random);
        return shuffledList;
    }
}
