package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;
import solvers.heuristicSolvers.beeColonyYu.BeeColonyUtils;

import java.util.Random;

public class InsertMove implements IMove {
    private final BeeColonyUtils beeColonyUtils;
    private final Random random;

    public InsertMove(BeeColonyUtils beeColonyUtils, Random random) {
        this.beeColonyUtils = beeColonyUtils;
        this.random = random;
    }

    public BeeColonySolution apply(BeeColonySolution solution) {
        return random(solution);
        //        return bestImproving(solution);
    }

    private BeeColonySolution bestImproving(BeeColonySolution solution) {
        var bestSolution = solution;
        var obj = 0.0;
        for (var i = 0; i < solution.getSolutionString().length; i++) {
            for (var j = 0; j < solution.getSolutionString().length; j++) {
                var newSolution = apply(solution, i, j);
                if (newSolution.getFitness() > obj) {
                    bestSolution = newSolution;
                    obj = newSolution.getFitness();
                }
            }
        }
        return bestSolution;
    }

    private BeeColonySolution random(BeeColonySolution solution) {
        var randoms =
                random.ints(0, solution.getSolutionString().length).distinct().limit(2).toArray();

        return apply(solution, randoms[0], randoms[1]);
    }

    private BeeColonySolution apply(BeeColonySolution solution, int indexOfFrom, int indexOfTo) {
        int[] originalArray = solution.getSolutionString();
        int[] newArray = new int[originalArray.length];

        System.arraycopy(originalArray, 0, newArray, 0, Math.min(indexOfFrom, indexOfTo));
        System.arraycopy(
                originalArray,
                Math.max(indexOfFrom, indexOfTo) + 1,
                newArray,
                Math.max(indexOfFrom, indexOfTo) + 1,
                originalArray.length - Math.max(indexOfFrom, indexOfTo) - 1);

        if (indexOfFrom < indexOfTo) {
            System.arraycopy(
                    originalArray, indexOfFrom + 1, newArray, indexOfFrom, indexOfTo - indexOfFrom);
        } else {
            System.arraycopy(
                    originalArray,
                    indexOfTo,
                    newArray,
                    indexOfTo + 1,
                    indexOfFrom - indexOfTo);
        }

        newArray[indexOfTo] = originalArray[indexOfFrom];

        return new BeeColonySolution(newArray, beeColonyUtils.calculateFitness(newArray));
    }
}
