package solvers.heuristicSolvers.beeColonyYu;

import data.Solution;
import data.SolutionData;

import java.util.ArrayList;

public class BeeColonySolution {
    private final int[] solutionString;
    private final double fitness;

    public BeeColonySolution(int[] solutionString, double fitness) {
        this.solutionString = solutionString;
        this.fitness = fitness;
    }

    public int[] getSolutionString() {
        return solutionString;
    }

    public double getFitness() {
        return fitness;
    }

}
