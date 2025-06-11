package solvers.mipSolvers;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;

import data.ProblemParameters;

import runParameters.MipRunSettings;

import solvers.CheckPoint;
import solvers.SolverSolution;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class ModelSolver {
    private final BaseModel model;
    private final ProblemParameters parameters;
    private final MipRunSettings runSettings;

    private final List<CheckPoint> checkPoints;
    private SolverSolution solution;

    public ModelSolver(BaseModel model, ProblemParameters parameters, MipRunSettings runSettings)
            throws GRBException {
        this.model = model;
        this.parameters = parameters;
        this.runSettings = runSettings;

        this.checkPoints = new ArrayList<>();

        solve();
        createSolverSolution();

        model.dispose();
    }

    private void solve() throws GRBException {
        model.build();

        model.getModel().set(GRB.IntParam.SolutionLimit, 1);
        model.getModel().set(GRB.DoubleParam.TimeLimit, runSettings.checkPointTimes().getLast());
        model.optimize();

        model.getModel().set(GRB.IntParam.SolutionLimit, GRB.MAXINT);

        double passedTime = model.getModel().get(GRB.DoubleAttr.Runtime);

        for (var checkPointTime : runSettings.checkPointTimes()) {
            if (passedTime >= checkPointTime) {
                continue;
            }

            this.model.getModel().set(GRB.DoubleParam.TimeLimit, checkPointTime - passedTime);
            this.model.optimize();

            if (model.getModel().get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                var sol = model.generateSolution();
                checkPoints.add(new CheckPoint(sol, model.getModel().get(GRB.DoubleAttr.Runtime)));
                break;
            }

            var sol = model.generateSolution();
            checkPoints.add(new CheckPoint(sol, checkPointTime));
            passedTime = checkPointTime;
        }
    }

    private void createSolverSolution() throws GRBException {
        this.solution =
                new SolverSolution(
                        checkPoints.getLast().getSolution(),
                        checkPoints,
                        new MipInformation(
                                runSettings.checkPointTimes().getLast(),
                                model.getModel().get(GRB.DoubleAttr.Runtime),
                                model.getModel().get(GRB.DoubleAttr.ObjVal),
                                model.getModel().get(GRB.DoubleAttr.MIPGap),
                                model.getModel().get(GRB.DoubleAttr.ObjBound)),
                        parameters.getInstance()
                );
    }

    public SolverSolution getSolution() {
        return solution;
    }
}
