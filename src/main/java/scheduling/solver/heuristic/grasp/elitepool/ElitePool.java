package scheduling.solver.heuristic.grasp.elitepool;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Random;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.pathrelinking.PathRelinkingUtils;

public class ElitePool {

    private final ArrayList<GraspSolution> solutions = new ArrayList<>();
    private final int maxSize;
    private final int numCommercials;

    public ElitePool(int maxSize, int numCommercials) {
        checkArgument(maxSize > 0, "maxSize must be positive");
        checkArgument(numCommercials >= 0, "numCommercials must be non-negative");
        this.maxSize = maxSize;
        this.numCommercials = numCommercials;
    }

    public static ElitePool threadSafe(int maxSize, int numCommercials) {
        return new ThreadSafeElitePool(maxSize, numCommercials);
    }

    public void add(GraspSolution candidate) {
        addInternal(candidate);
    }

    public GraspSolution getRandomGuide(Random random) {
        return solutions.get(random.nextInt(solutions.size()));
    }

    public int size() {
        return solutions.size();
    }

    private void addInternal(GraspSolution candidate) {
        var duplicateIndex = findDuplicateIndex(candidate);
        if (duplicateIndex >= 0) {
            if (candidate.getTotalRevenue() > solutions.get(duplicateIndex).getTotalRevenue()) {
                solutions.set(duplicateIndex, candidate);
            }
            return;
        }

        if (solutions.size() < maxSize) {
            solutions.add(candidate);
            return;
        }

        var worstIndex = findWorstIndex();
        if (candidate.getTotalRevenue() <= solutions.get(worstIndex).getTotalRevenue()) {
            return;
        }

        solutions.set(worstIndex, candidate);
    }

    private int findDuplicateIndex(GraspSolution candidate) {
        for (int i = 0; i < solutions.size(); i++) {
            if (PathRelinkingUtils.distance(candidate, solutions.get(i), numCommercials) == 0) {
                return i;
            }
        }
        return -1;
    }

    private int findWorstIndex() {
        var worstIndex = 0;
        var worstRevenue = solutions.getFirst().getTotalRevenue();
        for (int i = 1; i < solutions.size(); i++) {
            if (solutions.get(i).getTotalRevenue() < worstRevenue) {
                worstRevenue = solutions.get(i).getTotalRevenue();
                worstIndex = i;
            }
        }
        return worstIndex;
    }
}
