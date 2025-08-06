package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.GlobalRandom;
import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

public class SwapMove implements IMove{
    private final BeeColonyUtils beeColonyUtils;

    public SwapMove(BeeColonyUtils beeColonyUtils) {
        this.beeColonyUtils = beeColonyUtils;
    }

    public BeeColonySolution apply(BeeColonySolution solution){
        var randoms = GlobalRandom.getRandom().ints(0, solution.getSolutionString().length)
                .distinct()
                .limit(2)
                .toArray();

        return apply(solution, randoms[0], randoms[1]);
    }

    private BeeColonySolution apply(BeeColonySolution solution, int indexOfFrom, int indexOfTo) {
        int[] newSolutionString = new int[solution.getSolutionString().length];

        for (int i = 0; i < solution.getSolutionString().length; i++) {
            newSolutionString[i] = solution.getSolutionString()[i];
        }

        newSolutionString[indexOfFrom] = solution.getSolutionString()[indexOfTo];
        newSolutionString[indexOfTo] = solution.getSolutionString()[indexOfFrom];

        return new BeeColonySolution(newSolutionString, beeColonyUtils.calculateFitness(newSolutionString));
    }
}
