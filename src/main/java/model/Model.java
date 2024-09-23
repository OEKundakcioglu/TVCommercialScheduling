package model;

import com.gurobi.gurobi.*;
import data.Commercial;
import data.Inventory;
import data.Solution;
import data.SolutionData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import runParameters.MipRunSettings;

import java.util.*;

public class Model {
    private final Logger logger = LogManager.getLogger(Model.class);
    private final GRBModel model;
    private final GRBEnv env;
    private Variables variables;
    public MipSolution solution;
    public double revenue;
    private double firstFeasibleSolutionTime;
    private final ProblemParameters parameters;
    private final MipRunSettings runSettings;

    public Model(ProblemParameters parameters, MipRunSettings runSettings) throws Exception {
        this.env = new GRBEnv();
        this.model = new GRBModel(this.env);
        this.parameters = parameters;
        this.runSettings = runSettings;

        this.create();
        this.optimize();
        this.dispose();
    }

    private void create() throws Exception {
        this.logger.info("Creating variables...");
        this.variables = new Variables();
        this.variables.populateVariables(parameters, this.model);

        this.logger.info("Setting objective...");
        Objective.setObjective(parameters, this.model, this.variables);
        this.logger.info("Setting constraints...");
        Constraint.setConstraints(parameters, this.model, this.variables);
    }

    private void optimize() throws Exception {

        this.model.set(GRB.StringParam.LogFile, runSettings.logPath());
        this.model.set(GRB.DoubleParam.TimeLimit, runSettings.timeLimit());

        this.model.set(GRB.IntParam.SolutionLimit, 1);
        this.model.optimize();

        this.firstFeasibleSolutionTime = this.model.get(GRB.DoubleAttr.Runtime);
        this.model.set(GRB.IntParam.SolutionLimit, Integer.MAX_VALUE);
        this.model.optimize();

        if (this.model.get(GRB.IntAttr.SolCount) > 0) {
            generateSolution();
        }
        else {

            var emptySolution = new ArrayList<List<SolutionData>>();
            //noinspection unused
            for (var inventory : parameters.getSetOfInventories()) {
                emptySolution.add(new ArrayList<>());
            }

            this.solution = new MipSolution(
                    this.runSettings.timeLimit(),
                    this.runSettings.timeLimit(),
                    Double.MAX_VALUE,
                    Double.MAX_VALUE,
                    new Solution(emptySolution)
            );
        }

        if (this.model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
            this.model.computeIIS();
            for (GRBConstr c : this.model.getConstrs()) {
                if (c.get(GRB.IntAttr.IISConstr) == 1) {
                    this.logger.error(c.get(GRB.StringAttr.ConstrName));
                }
            }
        }
    }

    private void generateSolution() throws Exception {
        var solutions = new HashMap<Inventory, List<SolutionData>>();
        for (Commercial commercial : variables.getO().keySet()) {
            for (Inventory inventory : variables.getO().get(commercial).keySet()) {
                for (Map.Entry<Integer, GRBVar> pair : variables.getO().get(commercial).get(inventory).entrySet()) {
                    if (pair.getValue().get(GRB.DoubleAttr.X) > 0.5) {
                        double startTime = this.variables.getS().get(commercial).get(inventory).get(pair.getKey()).get(GRB.DoubleAttr.X);
                        var revenue = commercial.getRevenue(this.parameters.getRatings().
                                get(inventory).get((int) (startTime / 60) + 1).
                                get(commercial.getAudienceType())
                        );

                        var solutionData = new SolutionData(commercial, inventory);
                        solutionData.update(revenue, startTime, -1);

                        if (!solutions.containsKey(inventory)) {
                            solutions.put(inventory, new LinkedList<>());
                        }

                        solutions.get(inventory).add(solutionData);
                    }
                }
            }
        }

        solutions.forEach((inventory, solutionDataList) ->
                solutionDataList.sort(Comparator.comparing(SolutionData::getStartTime)));

        var solutionList = new ArrayList<List<SolutionData>>();
        for (var inventory : parameters.getSetOfInventories()) {
            solutionList.add(solutions.get(inventory));
        }

        for (var kvp : solutions.entrySet()){
            solutionList.set(kvp.getKey().getId(), kvp.getValue());
        }

        this.solution = new MipSolution(
                this.runSettings.timeLimit(),
                this.model.get(GRB.DoubleAttr.Runtime),
                this.model.get(GRB.DoubleAttr.MIPGap),
                this.firstFeasibleSolutionTime,
                new Solution(solutionList)
        );
    }

    private void dispose() throws GRBException {
        this.model.dispose();
        this.env.dispose();
    }

}
