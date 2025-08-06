package solvers.heuristicSolvers.beeColonyYu;

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
