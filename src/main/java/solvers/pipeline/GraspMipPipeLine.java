package solvers.pipeline;

import data.ProblemParameters;
import data.Solution;

import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.grasp.GraspInformation;
import solvers.heuristicSolvers.grasp.graspWithPathRelinking.GraspWithPathRelinking;
import solvers.mipSolvers.MipInformation;
import solvers.mipSolvers.ModelSolver;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

import java.util.ArrayList;
import java.util.List;

public class GraspMipPipeLine {
    private final ProblemParameters parameters;
    private final PipelineConfig config;
    private final List<CheckPoint> checkPoints;
    private PipelineSolution pipelineSolution;

    public GraspMipPipeLine(ProblemParameters parameters, PipelineConfig config) throws Exception {
        this.parameters = parameters;
        this.config = config;
        this.checkPoints = new ArrayList<>();
        solve();
    }

    public Solution solve() throws Exception {
        var graspSolution = solveWithGrasp();
        var mipSolution = solveWithMip(graspSolution.getBestSolution());

        checkPoints.addAll(graspSolution.getCheckPoints());

        int graspTimeLimit = config.graspSettings().timeLimit();
        for (var cp : mipSolution.getCheckPoints()) {
            checkPoints.add(new CheckPoint(cp.getObjective(), cp.getTime() + graspTimeLimit));
        }

        var pipelineInfo =
                new PipelineInformation(
                        (GraspInformation) graspSolution.getAdditionalInformation(),
                        (MipInformation) mipSolution.getAdditionalInformation());

        this.pipelineSolution =
                new PipelineSolution(
                        mipSolution.getBestSolution(),
                        checkPoints,
                        pipelineInfo,
                        parameters.getInstance());

        return mipSolution.getBestSolution();
    }

    private SolverSolution solveWithGrasp() throws Exception {
        return new GraspWithPathRelinking(parameters, config.graspSettings()).getSolution();
    }

    private SolverSolution solveWithMip(Solution warmStart) throws Exception {
        var model = new DiscreteTimeModel(parameters);
        return new ModelSolver(model, parameters, config.mipRunSettings(), warmStart).getSolution();
    }

    public PipelineSolution getPipelineSolution() {
        return pipelineSolution;
    }
}
