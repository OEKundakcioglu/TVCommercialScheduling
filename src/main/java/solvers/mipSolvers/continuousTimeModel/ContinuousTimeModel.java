package solvers.mipSolvers.continuousTimeModel;

import com.gurobi.gurobi.*;

import data.*;

import solvers.mipSolvers.BaseModel;

import java.util.*;

public class ContinuousTimeModel extends BaseModel {
    private final ProblemParameters parameters;
    public double revenue;
    private ContinuousTimeVariables variables;

    public ContinuousTimeModel(ProblemParameters parameters) throws Exception {
        this.parameters = parameters;
    }

    @Override
    public GRBModel getModel() {
        return this.model;
    }

    public void build() throws GRBException {
        this.logger.info("Creating variables...");
        this.variables = new ContinuousTimeVariables();
        this.variables.populateVariables(parameters, this.model);

        this.logger.info("Setting objective...");
        ContinuousTimeObjective.setObjective(parameters, this.model, this.variables);
        this.logger.info("Setting constraints...");
        ContinuousTimeConstraint.setConstraints(parameters, this.model, this.variables);
    }

    public Solution generateSolution() throws GRBException {
        if (this.model.get(GRB.IntAttr.SolCount) == 0) {
            return new Solution(List.of());
        }

        var solutions = new ArrayList<List<SolutionData>>();
        for (var inventory : parameters.getSetOfInventories()) {
            solutions.add(new ArrayList<>());
        }
        for (var n = 0; n < 100; n++) {
            for (Commercial commercial : variables.getO().keySet()) {
                for (Inventory inventory : variables.getO().get(commercial).keySet()) {
                    if (variables.getO().get(commercial).get(inventory).get(n) == null) continue;

                    if (variables.getO().get(commercial).get(inventory).get(n).get(GRB.DoubleAttr.X)
                            > 0.5) {

                        solutions
                                .get(inventory.getId())
                                .add(new SolutionData(commercial, inventory));
                    }
                }
            }
        }

        return new Solution(solutions);
    }

}
