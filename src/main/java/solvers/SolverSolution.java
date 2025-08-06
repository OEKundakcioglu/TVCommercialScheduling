package solvers;

import data.Solution;

import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class SolverSolution {
    private final Solution bestSolution;
    private final List<CheckPoint> checkPoints;
    private final String instance;
    private final Object additionalInformation;
    private final Long seed;

    public SolverSolution(Solution bestSolution, List<CheckPoint> checkPoints, Object additionalInformation, String instance) {
        this.bestSolution = bestSolution;
        this.checkPoints = checkPoints;
        this.additionalInformation = additionalInformation;
        this.instance = instance;
        seed = GlobalRandom.getSeed();
    }

    public Solution getBestSolution() {
        return bestSolution;
    }

    public List<CheckPoint> getCheckPoints() {
        return checkPoints;
    }

    public Object getAdditionalInformation() {
        return additionalInformation;
    }
}
