package grasp;

import data.Commercial;
import data.Inventory;
import data.Solution;
import runParameters.GraspSettings;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class GraspOutput {
    private final List<Commercial> commercials;
    private final List<Inventory> inventories;
    private final GraspSettings settings;
    private final List<CheckPoint> solutionMemory;
    private Solution bestSolution;
    private double avgIterationPerSecond;

    public GraspOutput(List<Commercial> commercials, List<Inventory> inventories, List<CheckPoint> solutionMemory, GraspSettings settings, Solution bestSolution) {
        this.commercials = commercials;
        this.inventories = inventories;
        this.solutionMemory = solutionMemory;
        this.settings = settings;
        this.bestSolution = bestSolution;
        this.avgIterationPerSecond = -1;
    }

    public List<CheckPoint> getSolutionMemory() {
        return solutionMemory;
    }

    public Solution getBestSolution() {
        return bestSolution;
    }

    public void setBestSolution(Solution bestSolution) {
        this.bestSolution = bestSolution;
    }

    public void setAvgIterationPerSecond(double avgIterationPerSecond) {
        this.avgIterationPerSecond = avgIterationPerSecond;
    }
}
