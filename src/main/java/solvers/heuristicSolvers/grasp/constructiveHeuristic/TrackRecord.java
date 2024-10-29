package solvers.heuristicSolvers.grasp.constructiveHeuristic;

import data.Inventory;

public class TrackRecord {
    protected Inventory inventory;
    protected int currentTime;
    protected int latestAiredCommercialsGroup;
    protected boolean isAnyAssigned;
    protected boolean isCommercialWithLastAttentionAssigned;
}
