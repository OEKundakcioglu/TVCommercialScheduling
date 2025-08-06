package solvers.mipSolvers;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import data.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseModel {
    protected final GRBEnv env;
    protected final GRBModel model;
    protected final Logger logger = LogManager.getLogger(getClass());

    public BaseModel() throws GRBException {
        this.env = new GRBEnv();
        this.model = new GRBModel(env);

        model.set(GRB.DoubleParam.SoftMemLimit, 120);
    }

    public abstract GRBModel getModel();
    public abstract void build() throws GRBException;
    public abstract Solution generateSolution() throws GRBException;
    public void dispose() throws GRBException {
        model.dispose();
        env.dispose();
    }

    public void optimize() throws GRBException {
        model.optimize();

        if (model.get(GRB.IntAttr.Status) == GRB.Status.MEM_LIMIT)
            throw new GRBException("Memory limit exceeded during optimization.");
    }

}
