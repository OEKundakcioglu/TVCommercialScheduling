package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

import java.util.Random;

public class SwapMove implements IMove{
    private final BeeColonyUtils beeColonyUtils;
    private final Random random;

    public SwapMove(BeeColonyUtils beeColonyUtils, Random random) {
        this.beeColonyUtils = beeColonyUtils;
        this.random = random;
    }

    public BeeColonySolution apply(BeeColonySolution solution){
        var randoms = random.ints(0, solution.getSolutionString().length)
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
