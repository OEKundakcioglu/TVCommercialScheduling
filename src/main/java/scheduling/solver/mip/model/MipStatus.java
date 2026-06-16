package scheduling.solver.mip.model;

import com.gurobi.gurobi.GRB;

final class MipStatus {

    private MipStatus() {}

    static String label(int statusCode) {
        return switch (statusCode) {
            case GRB.Status.LOADED -> "LOADED";
            case GRB.Status.OPTIMAL -> "OPTIMAL";
            case GRB.Status.INFEASIBLE -> "INFEASIBLE";
            case GRB.Status.INF_OR_UNBD -> "INF_OR_UNBD";
            case GRB.Status.UNBOUNDED -> "UNBOUNDED";
            case GRB.Status.CUTOFF -> "CUTOFF";
            case GRB.Status.ITERATION_LIMIT -> "ITERATION_LIMIT";
            case GRB.Status.NODE_LIMIT -> "NODE_LIMIT";
            case GRB.Status.TIME_LIMIT -> "TIME_LIMIT";
            case GRB.Status.SOLUTION_LIMIT -> "SOLUTION_LIMIT";
            case GRB.Status.INTERRUPTED -> "INTERRUPTED";
            case GRB.Status.NUMERIC -> "NUMERIC";
            case GRB.Status.SUBOPTIMAL -> "SUBOPTIMAL";
            case GRB.Status.INPROGRESS -> "INPROGRESS";
            case GRB.Status.USER_OBJ_LIMIT -> "USER_OBJ_LIMIT";
            case GRB.Status.WORK_LIMIT -> "WORK_LIMIT";
            case GRB.Status.MEM_LIMIT -> "MEM_LIMIT";
            case GRB.Status.LOCALLY_OPTIMAL -> "LOCALLY_OPTIMAL";
            case GRB.Status.LOCALLY_INFEASIBLE -> "LOCALLY_INFEASIBLE";
            default -> throw new IllegalArgumentException("Unknown Gurobi status: " + statusCode);
        };
    }
}
