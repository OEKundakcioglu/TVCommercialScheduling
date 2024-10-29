package solvers.mipSolvers;

import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import data.Solution;

public interface IModel {
    GRBModel getModel();
    void build() throws GRBException;
    Solution generateSolution() throws GRBException;
    void dispose() throws GRBException;
}
