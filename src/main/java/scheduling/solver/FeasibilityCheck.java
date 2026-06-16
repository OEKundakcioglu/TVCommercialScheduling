package scheduling.solver;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.solver.heuristic.grasp.GraspSolution;
import scheduling.solver.heuristic.grasp.SolutionConverter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FeasibilityCheck {

    private static final double REVENUE_ABSOLUTE_EPSILON = 1e-6;
    private static final double REVENUE_RELATIVE_EPSILON = 1e-7;

    public static void check(Problem problem, Solution solution) {
        var assignments = solution.getAssignments();
        checkSuitability(problem, assignments);
        checkAttention(problem, assignments);
        checkGroup(assignments);
        checkDuration(assignments);
        checkCommercialCount(assignments);
        checkHourlyLimit(assignments);
        checkUniqueAssignment(assignments);
        checkTotalRevenue(problem, solution);
    }

    public static void check(Problem problem, GraspSolution graspSolution) {
        check(problem, SolutionConverter.toSolution(problem, graspSolution));
    }

    private static void checkSuitability(
            Problem problem, Map<Inventory, List<Commercial>> assignments) {
        for (var entry : assignments.entrySet()) {
            var inventory = entry.getKey();
            for (var commercial : entry.getValue()) {
                checkState(
                        problem.isSuitable(commercial.getId(), inventory.getId()),
                        "Commercial %s is not suitable for inventory %s",
                        commercial.getId(),
                        inventory.getId());
            }
        }
    }

    private static void checkAttention(
            Problem problem, Map<Inventory, List<Commercial>> assignments) {
        for (var entry : assignments.entrySet()) {
            var inventory = entry.getKey();
            var commercials = entry.getValue();
            var sequenceLength = commercials.size();
            for (int position = 0; position < sequenceLength; position++) {
                var commercial = commercials.get(position);
                var types = problem.getAttentionTypes(commercial.getId(), inventory.getId());
                checkState(
                        AttentionType.anySatisfied(types, position, sequenceLength),
                        "Commercial %s at position %s in inventory %s violates all"
                                + " attention types (sequenceLength=%s)",
                        commercial.getId(),
                        position,
                        inventory.getId(),
                        sequenceLength);
            }
        }
    }

    private static void checkGroup(Map<Inventory, List<Commercial>> assignments) {
        for (var entry : assignments.entrySet()) {
            var commercials = entry.getValue();
            for (int i = 0; i < commercials.size() - 1; i++) {
                var current = commercials.get(i);
                var next = commercials.get(i + 1);
                checkState(
                        current.getGroup() != next.getGroup(),
                        "Adjacent commercials %s and %s in inventory %s have same group %s",
                        current.getId(),
                        next.getId(),
                        entry.getKey().getId(),
                        current.getGroup());
            }
        }
    }

    private static void checkDuration(Map<Inventory, List<Commercial>> assignments) {
        for (var entry : assignments.entrySet()) {
            var inventory = entry.getKey();
            var totalDuration = entry.getValue().stream().mapToInt(Commercial::getDuration).sum();
            checkState(
                    totalDuration <= inventory.getDuration(),
                    "Inventory %s total commercial duration %s exceeds capacity %s",
                    inventory.getId(),
                    totalDuration,
                    inventory.getDuration());
        }
    }

    private static void checkCommercialCount(Map<Inventory, List<Commercial>> assignments) {
        for (var entry : assignments.entrySet()) {
            var inventory = entry.getKey();
            var count = entry.getValue().size();
            checkState(
                    count <= inventory.getMaxCommercialCount(),
                    "Inventory %s has %s commercials, exceeding max allowed %s",
                    inventory.getId(),
                    count,
                    inventory.getMaxCommercialCount());
        }
    }

    private static void checkHourlyLimit(Map<Inventory, List<Commercial>> assignments) {
        var hourlyDuration = new HashMap<Integer, Integer>();
        for (var entry : assignments.entrySet()) {
            var hour = entry.getKey().getHour();
            var totalDuration = entry.getValue().stream().mapToInt(Commercial::getDuration).sum();
            hourlyDuration.merge(hour, totalDuration, Integer::sum);
        }
        for (var entry : hourlyDuration.entrySet()) {
            checkState(
                    entry.getValue() <= Problem.HOURLY_BROADCAST_LIMIT,
                    "Hour %s total commercial duration %s exceeds hourly limit %s",
                    entry.getKey(),
                    entry.getValue(),
                    Problem.HOURLY_BROADCAST_LIMIT);
        }
    }

    private static void checkUniqueAssignment(Map<Inventory, List<Commercial>> assignments) {
        var seen = new HashSet<Integer>();
        for (var entry : assignments.entrySet()) {
            for (var commercial : entry.getValue()) {
                checkState(
                        seen.add(commercial.getId()),
                        "Commercial %s is assigned to multiple inventories",
                        commercial.getId());
            }
        }
    }

    private static void checkTotalRevenue(Problem problem, Solution solution) {
        var expectedRevenue = 0.0;
        for (var entry : solution.getAssignments().entrySet()) {
            var inventory = entry.getKey();
            var startTime = 0;
            for (var commercial : entry.getValue()) {
                expectedRevenue +=
                        problem.getRevenue(commercial.getId(), inventory.getId(), startTime);
                startTime += commercial.getDuration();
            }
        }
        var actualRevenue = solution.getTotalRevenue();
        var difference = Math.abs(expectedRevenue - actualRevenue);
        var allowedDifference =
                Math.max(
                        REVENUE_ABSOLUTE_EPSILON,
                        REVENUE_RELATIVE_EPSILON
                                * Math.max(Math.abs(expectedRevenue), Math.abs(actualRevenue)));
        checkState(
                difference <= allowedDifference,
                "Total revenue mismatch: expected %s but solution has %s (diff=%s, allowed=%s)",
                expectedRevenue,
                actualRevenue,
                difference,
                allowedDifference);
    }
}
