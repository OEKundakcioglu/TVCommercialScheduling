package solvers.pipeline;

import data.Solution;

import solvers.CheckPoint;
import solvers.SolverSolution;

import java.util.List;

public class PipelineSolution extends SolverSolution {
    public PipelineSolution(
            Solution bestSolution,
            List<CheckPoint> checkPoints,
            Object additionalInformation,
            String instance) {
        super(bestSolution, checkPoints, additionalInformation, instance);
    }
}
