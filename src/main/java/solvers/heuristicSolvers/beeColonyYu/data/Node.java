package solvers.heuristicSolvers.beeColonyYu.data;

public interface Node {
    int getId();
    double distanceTo(Node other, Vehicle vehicle);
}
