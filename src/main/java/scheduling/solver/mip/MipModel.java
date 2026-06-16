package scheduling.solver.mip;

import scheduling.model.Problem;
import scheduling.solver.SolverSolution;

public interface MipModel extends AutoCloseable {

    void build(Problem problem);

    void optimize();

    SolverSolution<MipInformation> extractSolution();

    @Override
    void close();
}
