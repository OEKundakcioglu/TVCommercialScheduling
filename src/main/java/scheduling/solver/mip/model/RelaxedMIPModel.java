package scheduling.solver.mip.model;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.solver.Solution;
import scheduling.solver.SolverSolution;
import scheduling.solver.mip.RelaxedMIPConfig;
import scheduling.solver.mip.RelaxedMIPInformation;

@SuppressWarnings("NullAway.Init")
public class RelaxedMIPModel implements AutoCloseable {

    private final RelaxedMIPConfig config;
    private GRBEnv env;
    private GRBModel grbModel;
    private RelaxedMIPVariables variables;
    private Problem problem;
    private final MipCheckpointRecorder checkpointRecorder = new MipCheckpointRecorder();

    public RelaxedMIPModel(RelaxedMIPConfig config) {
        this.config = config;
    }

    public void build(Problem problem) {
        this.problem = problem;
        try {
            env = new GRBEnv();
            grbModel = new GRBModel(env);

            variables = new RelaxedMIPVariables(grbModel, problem);
            grbModel.update();

            new RelaxedMIPObjective(grbModel, problem, variables, config.returnMode())
                    .setObjective();
            new RelaxedMIPConstraints(grbModel, problem, variables).setConstraints();
        } catch (GRBException e) {
            throw new RuntimeException("Failed to build relaxed MIP model", e);
        }
    }

    public void optimize() {
        var callback = new IncumbentCheckpointCallback(checkpointRecorder);
        try {
            grbModel.set(GRB.DoubleParam.MIPGap, config.mipGap());
            grbModel.setCallback(callback);
            grbModel.optimize();
            callback.getFailure()
                    .ifPresent(
                            failure -> {
                                throw new RuntimeException(
                                        "Failed to record relaxed MIP checkpoint", failure);
                            });
        } catch (GRBException e) {
            throw new RuntimeException("Failed to optimize relaxed MIP model", e);
        }
    }

    public SolverSolution<RelaxedMIPInformation> extractSolution() {
        try {
            int solCount = grbModel.get(GRB.IntAttr.SolCount);
            if (solCount == 0) {
                throw new RuntimeException("No feasible solution found");
            }

            Map<Inventory, List<Commercial>> assignments = new LinkedHashMap<>();

            for (Inventory inv : problem.getInventories()) {
                List<Commercial> sequence = new ArrayList<>();
                int i = inv.getId();

                for (int n = 0; n < inv.getMaxCommercialCount(); n++) {
                    for (int commId : problem.getSuitableCommercials(i)) {
                        double val = variables.getO(commId, i, n).get(GRB.DoubleAttr.X);
                        if (val > 0.5) {
                            sequence.add(problem.getCommercial(commId));
                            break;
                        }
                    }
                }

                if (!sequence.isEmpty()) {
                    assignments.put(inv, sequence);
                }
            }

            double relaxedIncumbentValue = grbModel.get(GRB.DoubleAttr.ObjVal);
            double realizedRevenue = ContinuousTimeModel.calculateRevenue(problem, assignments);
            int statusCode = grbModel.get(GRB.IntAttr.Status);
            checkpointRecorder.record(relaxedIncumbentValue, grbModel.get(GRB.DoubleAttr.Runtime));
            Solution solution = new Solution(assignments, realizedRevenue);
            var relaxedMIPInformation =
                    new RelaxedMIPInformation(
                            config,
                            statusCode,
                            MipStatus.label(statusCode),
                            relaxedIncumbentValue,
                            grbModel.get(GRB.DoubleAttr.ObjBound),
                            realizedRevenue,
                            grbModel.get(GRB.DoubleAttr.MIPGap),
                            grbModel.get(GRB.DoubleAttr.Runtime),
                            grbModel.get(GRB.DoubleAttr.NodeCount),
                            solCount);
            return new SolverSolution<>(
                    solution, checkpointRecorder.snapshot(), relaxedMIPInformation);
        } catch (GRBException e) {
            throw new RuntimeException("Failed to extract relaxed MIP solution", e);
        }
    }

    @Override
    public void close() {
        if (grbModel != null) {
            grbModel.dispose();
        }
        if (env != null) {
            try {
                env.dispose();
            } catch (GRBException e) {
                throw new RuntimeException("Failed to dispose Gurobi environment", e);
            }
        }
    }

    private static class IncumbentCheckpointCallback extends GRBCallback {

        private final MipCheckpointRecorder checkpointRecorder;
        private Optional<GRBException> failure = Optional.empty();

        IncumbentCheckpointCallback(MipCheckpointRecorder checkpointRecorder) {
            this.checkpointRecorder = checkpointRecorder;
        }

        @Override
        protected void callback() {
            if (where != GRB.CB_MIPSOL) {
                return;
            }

            try {
                var objective = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
                var runtime = getDoubleInfo(GRB.CB_RUNTIME);
                checkpointRecorder.record(objective, runtime);
            } catch (GRBException e) {
                failure = Optional.of(e);
                abort();
            }
        }

        Optional<GRBException> getFailure() {
            return failure;
        }
    }
}
