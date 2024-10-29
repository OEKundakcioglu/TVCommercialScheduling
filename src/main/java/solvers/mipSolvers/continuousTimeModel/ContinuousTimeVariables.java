package solvers.mipSolvers.continuousTimeModel;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import data.Commercial;
import data.Inventory;
import data.ProblemParameters;

import java.util.HashMap;
import java.util.Map;

public class ContinuousTimeVariables {
    private Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> O;
    private Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> Z;
    private Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> S;

    public void populateVariables(ProblemParameters parameters, GRBModel model) throws GRBException {
        populateO(parameters, model);
        populateZ(parameters, model);
        populateS(parameters, model);
    }

    private void populateO(ProblemParameters parameters, GRBModel model) throws GRBException {
        this.O = new HashMap<>();
        for (Commercial commercial : parameters.getSetOfCommercials()) {
            this.O.put(commercial, new HashMap<>());
            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                this.O.get(commercial).put(inventory, new HashMap<>());
                for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                    GRBVar o = model.addVar(0, 1, 0, GRB.BINARY, String.format("o[%d][%d][%d]", commercial.getId(), inventory.getId(), n));
                    this.O.get(commercial).get(inventory).put(n, o);
                }
            }
        }
    }

    private void populateZ(ProblemParameters parameters, GRBModel model) throws GRBException {
        this.Z = new HashMap<>();
        for (Commercial commercial : parameters.getSetOfCommercials()) {
            this.Z.put(commercial, new HashMap<>());
            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                this.Z.get(commercial).put(inventory, new HashMap<>());
                for (int t = 1; t <= inventory.getDurationInMinutes(); t++) {
                    GRBVar z = model.addVar(0, 1, 0, GRB.BINARY, String.format("z[%d][%d][%d]", commercial.getId(), inventory.getId(), t));
                    this.Z.get(commercial).get(inventory).put(t, z);
                }
            }
        }
    }

    private void populateS(ProblemParameters parameters, GRBModel model) throws GRBException {
        this.S = new HashMap<>();
        for (Commercial commercial : parameters.getSetOfCommercials()) {
            this.S.put(commercial, new HashMap<>());
            for (Inventory inventory : commercial.getSetOfSuitableInv()) {
                this.S.get(commercial).put(inventory, new HashMap<>());
                for (int n = 0; n < inventory.getMaxCommercialCount(); n++) {
                    GRBVar s = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, String.format("S[%d][%d][%d]", commercial.getId(), inventory.getId(), n));
                    this.S.get(commercial).get(inventory).put(n, s);
                }
            }
        }
    }

    public Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> getO() {
        return O;
    }

    public Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> getZ() {
        return Z;
    }

    public Map<Commercial, Map<Inventory, Map<Integer, GRBVar>>> getS() {
        return S;
    }
}
