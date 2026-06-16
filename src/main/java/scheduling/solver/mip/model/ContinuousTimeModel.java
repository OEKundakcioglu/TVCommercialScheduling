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
import scheduling.solver.mip.MipConfig;
import scheduling.solver.mip.MipInformation;
import scheduling.solver.mip.MipModel;

@SuppressWarnings("NullAway.Init")
public class ContinuousTimeModel implements MipModel {

    private final MipConfig config;
    private GRBEnv env;
    private GRBModel grbModel;
    private ContinuousTimeVariables variables;
    private Problem problem;
    private final MipCheckpointRecorder checkpointRecorder = new MipCheckpointRecorder();

    public ContinuousTimeModel(MipConfig config) {
        this.config = config;
    }

    @Override
    public void build(Problem problem) {
        this.problem = problem;
        try {
            env = new GRBEnv();
            grbModel = new GRBModel(env);

            variables = new ContinuousTimeVariables(grbModel, problem);
            grbModel.update();

            new ContinuousTimeObjective(grbModel, problem, variables).setObjective();
            new ContinuousTimeConstraints(grbModel, problem, variables).setConstraints();
        } catch (GRBException e) {
            throw new RuntimeException("Failed to build MIP model", e);
        }
    }

    @Override
    public void optimize() {
        var callback = new IncumbentCheckpointCallback(checkpointRecorder);
        try {
            grbModel.set(GRB.DoubleParam.TimeLimit, config.timeLimitSeconds());
            grbModel.setCallback(callback);
            grbModel.optimize();
            callback.getFailure()
                    .ifPresent(
                            failure -> {
                                throw new RuntimeException(
                                        "Failed to record MIP checkpoint", failure);
                            });
        } catch (GRBException e) {
            throw new RuntimeException("Failed to optimize MIP model", e);
        }
    }

    @Override
    public SolverSolution<MipInformation> extractSolution() {
        try {
            int solCount = grbModel.get(GRB.IntAttr.SolCount);
            if (solCount == 0) {
                throw new RuntimeException("No feasible solution found within the time limit");
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

            double solverObjective = grbModel.get(GRB.DoubleAttr.ObjVal);
            int statusCode = grbModel.get(GRB.IntAttr.Status);
            checkpointRecorder.record(solverObjective, grbModel.get(GRB.DoubleAttr.Runtime));
            Solution solution = new Solution(assignments, calculateRevenue(problem, assignments));
            var mipInformation =
                    new MipInformation(
                            config,
                            statusCode,
                            MipStatus.label(statusCode),
                            solverObjective,
                            grbModel.get(GRB.DoubleAttr.ObjBound),
                            grbModel.get(GRB.DoubleAttr.MIPGap),
                            grbModel.get(GRB.DoubleAttr.Runtime),
                            grbModel.get(GRB.DoubleAttr.NodeCount),
                            solCount);
            return new SolverSolution<>(solution, checkpointRecorder.snapshot(), mipInformation);
        } catch (GRBException e) {
            throw new RuntimeException("Failed to extract solution", e);
        }
    }

    static double calculateRevenue(Problem problem, Map<Inventory, List<Commercial>> assignments) {
        double totalRevenue = 0.0;
        for (var entry : assignments.entrySet()) {
            var inventory = entry.getKey();
            var startTime = 0;
            for (var commercial : entry.getValue()) {
                totalRevenue +=
                        problem.getRevenue(commercial.getId(), inventory.getId(), startTime);
                startTime += commercial.getDuration();
            }
        }
        return totalRevenue;
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
