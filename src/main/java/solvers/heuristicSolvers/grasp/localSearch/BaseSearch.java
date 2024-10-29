package solvers.heuristicSolvers.grasp.localSearch;

import data.Solution;
import solvers.heuristicSolvers.grasp.localSearch.move.IMove;
import data.ProblemParameters;

import java.util.*;

public abstract class BaseSearch {
    protected Solution currentSolution;
    protected Solution bestFoundSolution;
    protected ProblemParameters parameters;
    protected final List<IMove> neighborhood;
    protected boolean getAllNeighborhood;
    protected boolean isBestMove;
    protected Random random;

    BaseSearch(Solution currentSolution, ProblemParameters parameters, boolean getAllNeighborhood, boolean isBestMove, Random random) {
        this.random = random;

        assert !isBestMove || !getAllNeighborhood;

        this.currentSolution = currentSolution;
        this.parameters = parameters;
        this.getAllNeighborhood = getAllNeighborhood;
        this.bestFoundSolution = null;
        this.isBestMove = isBestMove;
        this.neighborhood = new ArrayList<>();
    }

    protected boolean update(IMove move) throws Exception {
        var revenueGain = move.calculateRevenueGain();

        if (this.bestFoundSolution == null) this.bestFoundSolution = move.applyMove();

        if (this.getAllNeighborhood) {
            neighborhood.add(move);
        }

        else {
            if (revenueGain + currentSolution.revenue > bestFoundSolution.revenue) {
                bestFoundSolution = move.applyMove();
                return isBestMove;
            }
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

    protected List<Integer> getShuffledIndexList(int start, int excludedEnd){
        var shuffledList = new ArrayList<Integer>();
        for (var i = start; i < excludedEnd; i++) {
            shuffledList.add(i);
        }
        Collections.shuffle(shuffledList, random);
        return shuffledList;
    }
}
