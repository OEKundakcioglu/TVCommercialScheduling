package model;

import data.Solution;

public record MipSolution(
        int timeLimit,
        double runTime,
        double gap,
        double firstFeasibleSolutionTime,
        Solution solution
) {
}
