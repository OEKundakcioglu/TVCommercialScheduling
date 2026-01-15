package solvers.heuristicSolvers.grasp.reactiveGrasp;

import java.util.Random;

public interface AlphaGenerator {
    double generateAlpha(Random random);

    String getStringIdentifier();

    @Override
    int hashCode();
}
