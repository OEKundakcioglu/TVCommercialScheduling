package scheduling.solver.heuristic.grasp.construction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.solver.heuristic.grasp.GraspSolution;

public class GreedyConstruction {

    private final Problem problem;
    private final double alpha;
    private final Random random;
    private final double lowerBound;
    private final double upperBound;

    private record Candidate(int commId, int invId, double score) {}

    private List<List<Integer>> sequences;
    private int[] currentTime;
    private int[] lastCommId;
    private int[] totalDurationOfHour;
    private double[] randomFactor;
    private Set<Integer> unassigned;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Random is intentionally shared")
    public GreedyConstruction(
            Problem problem, double alpha, Random random, double lowerBound, double upperBound) {
        this.problem = problem;
        this.alpha = alpha;
        this.random = random;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;

        var numInv = problem.getInventories().length;
        sequences = buildSequences(numInv);
        currentTime = new int[numInv];
        lastCommId = new int[numInv];
        Arrays.fill(lastCommId, -1);
        var maxHour = Arrays.stream(problem.getHours()).max().orElse(0);
        totalDurationOfHour = new int[maxHour + 1];
        randomFactor = new double[0];
        unassigned = buildUnassignedSet();
    }

    public GraspSolution solve() {
        initializeState();

        while (true) {
            var candidate = selectCandidate();
            if (candidate.isEmpty()) {
                break;
            }
            appendCandidate(candidate.get());
        }

        return buildGraspSolution();
    }

    private void initializeState() {
        var numInv = problem.getInventories().length;
        var numComm = problem.getCommercials().length;
        sequences = buildSequences(numInv);
        currentTime = new int[numInv];
        lastCommId = new int[numInv];
        Arrays.fill(lastCommId, -1);
        var maxHour = Arrays.stream(problem.getHours()).max().orElse(0);
        totalDurationOfHour = new int[maxHour + 1];
        randomFactor = new double[numComm];
        for (int c = 0; c < numComm; c++) {
            randomFactor[c] = random.nextDouble(lowerBound, upperBound);
        }
        unassigned = buildUnassignedSet();
    }

    private Optional<Candidate> selectCandidate() {
        var bestScore = Double.NEGATIVE_INFINITY;
        var worstScore = Double.POSITIVE_INFINITY;
        var candidates = new ArrayList<Candidate>();

        for (int commId : unassigned) {
            for (int invId : problem.getSuitableInventories(commId)) {
                if (!isFeasible(commId, invId)) {
                    continue;
                }
                var score = calculateGreedyScore(commId, invId);
                candidates.add(new Candidate(commId, invId, score));
                bestScore = Math.max(bestScore, score);
                worstScore = Math.min(worstScore, score);
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        var threshold = bestScore - alpha * (bestScore - worstScore);
        var rcl = new ArrayList<Candidate>();
        for (var candidate : candidates) {
            if (candidate.score() >= threshold) {
                rcl.add(candidate);
            }
        }
        return Optional.of(rcl.get(random.nextInt(rcl.size())));
    }

    private double calculateGreedyScore(int commId, int invId) {
        return problem.getRevenue(commId, invId, currentTime[invId]) * randomFactor[commId];
    }

    private boolean isFeasible(int commId, int invId) {
        if (sequences.get(invId).size() >= problem.getInventory(invId).getMaxCommercialCount()) {
            return false;
        }

        var position = sequences.get(invId).size();
        if (!isAttentionSatisfied(commId, invId, position)) {
            return false;
        }

        if (appendBreaksExisting(invId)) {
            return false;
        }

        if (isSameGroupAsLast(commId, invId)) {
            return false;
        }

        if (exceedsInventoryDuration(commId, invId)) {
            return false;
        }

        if (exceedsHourlyBroadcastLimit(commId, invId)) {
            return false;
        }

        return true;
    }

    private boolean appendBreaksExisting(int invId) {
        var seq = sequences.get(invId);
        var currentLen = seq.size();
        var newLen = currentLen + 1;
        for (int pos = Math.max(0, currentLen - 3); pos < currentLen; pos++) {
            if (!AttentionType.anySatisfied(
                    problem.getAttentionTypes(seq.get(pos), invId), pos, newLen)) {
                return true;
            }
        }
        return false;
    }

    private boolean exceedsInventoryDuration(int commId, int invId) {
        return currentTime[invId] + problem.getCommercial(commId).getDuration()
                > problem.getInventory(invId).getDuration();
    }

    private boolean exceedsHourlyBroadcastLimit(int commId, int invId) {
        var hour = problem.getInventory(invId).getHour();
        return totalDurationOfHour[hour] + problem.getCommercial(commId).getDuration()
                > Problem.HOURLY_BROADCAST_LIMIT;
    }

    private boolean isSameGroupAsLast(int commId, int invId) {
        return lastCommId[invId] >= 0
                && problem.getCommercial(lastCommId[invId]).getGroup()
                        == problem.getCommercial(commId).getGroup();
    }

    private boolean isAttentionSatisfied(int commId, int invId, int position) {
        return AttentionType.anySatisfied(
                problem.getAttentionTypes(commId, invId), position, position + 1);
    }

    private void appendCandidate(Candidate candidate) {
        var commId = candidate.commId();
        var invId = candidate.invId();
        var comm = problem.getCommercial(commId);

        sequences.get(invId).add(commId);
        currentTime[invId] += comm.getDuration();
        lastCommId[invId] = commId;
        totalDurationOfHour[problem.getInventory(invId).getHour()] += comm.getDuration();
        unassigned.remove(commId);
    }

    private List<List<Integer>> buildSequences(int numInv) {
        var result = new ArrayList<List<Integer>>(numInv);
        for (int i = 0; i < numInv; i++) {
            result.add(new ArrayList<>());
        }
        return result;
    }

    private Set<Integer> buildUnassignedSet() {
        var result = new LinkedHashSet<Integer>();
        for (var comm : problem.getCommercials()) {
            result.add(comm.getId());
        }
        return result;
    }

    private GraspSolution buildGraspSolution() {
        var numInv = sequences.size();
        var numComm = problem.getCommercials().length;
        var seqArrays = new int[numInv][];
        var startTimes = new int[numInv][];
        var revenues = new double[numInv][];
        var totalRevenue = 0.0;
        var assignedInvId = new int[numComm];
        var assignedPos = new int[numComm];
        Arrays.fill(assignedInvId, -1);
        Arrays.fill(assignedPos, -1);

        for (int invId = 0; invId < numInv; invId++) {
            var seq = sequences.get(invId);
            seqArrays[invId] = seq.stream().mapToInt(Integer::intValue).toArray();
            startTimes[invId] = new int[seq.size()];
            revenues[invId] = new double[seq.size()];

            var time = 0;
            for (int pos = 0; pos < seq.size(); pos++) {
                var commId = seq.get(pos);
                startTimes[invId][pos] = time;
                var rev = problem.getRevenue(commId, invId, time);
                revenues[invId][pos] = rev;
                totalRevenue += rev;
                time += problem.getCommercial(commId).getDuration();
                assignedInvId[commId] = invId;
                assignedPos[commId] = pos;
            }
        }

        return new GraspSolution(
                seqArrays,
                startTimes,
                revenues,
                totalRevenue,
                totalDurationOfHour,
                currentTime,
                assignedInvId,
                assignedPos);
    }
}
