package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

import java.util.Random;

public class InversionMove implements IMove{
    private final BeeColonyUtils beeColonyUtils;
    private final Random random;

    public InversionMove(BeeColonyUtils beeColonyUtils, Random random) {
        this.beeColonyUtils = beeColonyUtils;
        this.random = random;
    }

    public BeeColonySolution apply(BeeColonySolution solution){
        var low = random.nextInt(0, solution.getSolutionString().length-1);
        var high = random.nextInt(low + 1, solution.getSolutionString().length);

        return apply(solution, low, high);
    }

    private BeeColonySolution apply(BeeColonySolution solution, int low, int high) {
        int[] newSolutionString = new int[solution.getSolutionString().length];

        for (int i = 0; i < low; i++) {
            newSolutionString[i] = solution.getSolutionString()[i];
        }

        for (int i = low, j = high; i <= high; i++, j--) {
            newSolutionString[i] = solution.getSolutionString()[j];
        }

        for (int i = high + 1; i < solution.getSolutionString().length; i++) {
            newSolutionString[i] = solution.getSolutionString()[i];
        }

        return new BeeColonySolution(newSolutionString, beeColonyUtils.calculateFitness(newSolutionString));
    }
}
