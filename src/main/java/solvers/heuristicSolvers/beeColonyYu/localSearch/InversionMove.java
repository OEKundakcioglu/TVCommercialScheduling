package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.GlobalRandom;
import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

public class InversionMove implements IMove{
    private final BeeColonyUtils beeColonyUtils;

    public InversionMove(BeeColonyUtils beeColonyUtils) {
        this.beeColonyUtils = beeColonyUtils;
    }

    public BeeColonySolution apply(BeeColonySolution solution){
        var low = GlobalRandom.getRandom().nextInt(0, solution.getSolutionString().length - 1);
        var high = GlobalRandom.getRandom().nextInt(low + 1, solution.getSolutionString().length);

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
