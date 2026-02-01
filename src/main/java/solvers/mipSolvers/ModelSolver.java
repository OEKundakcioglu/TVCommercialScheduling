package solvers.mipSolvers;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;

import data.ProblemParameters;
import data.Solution;

import runParameters.MipRunSettings;

import solvers.SolverSolution;
import solvers.mipSolvers.discreteTimeModel.DiscreteTimeModel;

@SuppressWarnings("FieldCanBeLocal")
public class ModelSolver {
    private final BaseModel model;
    private final ProblemParameters parameters;
    private final MipRunSettings runSettings;
    private final Solution warmStart;

    private MipCallback callback;
    private SolverSolution solution;

    public ModelSolver(BaseModel model, ProblemParameters parameters, MipRunSettings runSettings)
            throws GRBException {
        this(model, parameters, runSettings, null);
    }

    public ModelSolver(
            BaseModel model,
            ProblemParameters parameters,
            MipRunSettings runSettings,
            Solution warmStart)
            throws GRBException {
        this.model = model;
        this.parameters = parameters;
        this.runSettings = runSettings;
        this.warmStart = warmStart;

        solve();
        createSolverSolution();

        model.dispose();
    }

    private void solve() throws GRBException {
        model.build();

        if (warmStart != null && model instanceof DiscreteTimeModel dtm) {
            dtm.giveWarmStart(warmStart);
        }

        // Create and attach callback
        this.callback = new MipCallback();
        this.model.getModel().setCallback(callback);

        // Set time limit
        this.model.getModel().set(GRB.DoubleParam.TimeLimit, runSettings.timeLimit());

        // Single optimize call
        this.model.optimize();
    }

    private void createSolverSolution() throws GRBException {
        this.solution =
                new SolverSolution(
                        model.generateSolution(),
                        callback.getCheckPoints(),
                        new MipInformation(
                                runSettings.timeLimit(),
                                model.getModel().get(GRB.DoubleAttr.Runtime),
                                model.getModel().get(GRB.IntAttr.SolCount) > 0
                                        ? model.getModel().get(GRB.DoubleAttr.ObjVal)
                                        : 0.0,
                                model.getModel().get(GRB.IntAttr.SolCount) > 0
                                        ? model.getModel().get(GRB.DoubleAttr.MIPGap)
                                        : Double.POSITIVE_INFINITY,
                                model.getModel().get(GRB.DoubleAttr.ObjBound)),
                        parameters.getInstance());
    }

    public SolverSolution getSolution() {
        return solution;
    }
}
