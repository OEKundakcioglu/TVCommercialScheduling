package solvers.heuristicSolvers.beeColonyYu.localSearch;

import solvers.heuristicSolvers.beeColonyYu.BeeColonySolution;

public interface IMove {
    BeeColonySolution apply(BeeColonySolution beeColonySolution);
}
