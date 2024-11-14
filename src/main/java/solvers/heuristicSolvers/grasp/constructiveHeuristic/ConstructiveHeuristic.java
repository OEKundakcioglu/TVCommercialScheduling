package solvers.heuristicSolvers.grasp.constructiveHeuristic;

import data.*;
import data.ProblemParameters;
import data.enums.ATTENTION;

import runParameters.ConstructiveHeuristicSettings;

import java.util.*;

public class ConstructiveHeuristic implements IConstructiveHeuristic {
    private final double firstAttentionBoost;
    private final double lastAttentionBoost;
    private final double f30AttentionBoost;
    private final double f60AttentionBoost;
    private final ProblemParameters parameters;
    private final Random random;
    private final double alpha;
    private final List<List<Commercial>> solutionList;
    private final List<Commercial> unassignedCommercials;
    private final Map<Inventory, TrackRecord> trackRecordMap;
    private final int[] totalCommercialTimes;
    private final int smallestHour;
    private Solution solution;

    public ConstructiveHeuristic(ProblemParameters parameters, double alpha, Random random, ConstructiveHeuristicSettings constructiveHeuristicSettings) {
        this.random = random;
        this.alpha = alpha;

        this.firstAttentionBoost = random.nextDouble(constructiveHeuristicSettings.lowerBound(), constructiveHeuristicSettings.upperBound());
        this.lastAttentionBoost = 1 / random.nextDouble(constructiveHeuristicSettings.lowerBound(), constructiveHeuristicSettings.upperBound());
        this.f30AttentionBoost = random.nextDouble(constructiveHeuristicSettings.lowerBound(), constructiveHeuristicSettings.upperBound());
        this.f60AttentionBoost = random.nextDouble(constructiveHeuristicSettings.lowerBound(), constructiveHeuristicSettings.upperBound());

        this.parameters = parameters;
        this.unassignedCommercials = new ArrayList<>(this.parameters.getSetOfCommercials());
        this.totalCommercialTimes = new int[this.parameters.getSetOfHours().size()];
        this.smallestHour = this.parameters.getSetOfHours().stream().mapToInt(Integer::intValue).min().orElse(0);

        this.solutionList = new ArrayList<>();

        this.parameters.getSetOfInventories()
                .forEach(inv -> this.solutionList.add(inv.getId(), new ArrayList<>()));

        this.trackRecordMap = new HashMap<>();
        for (Inventory inventory : this.parameters.getSetOfInventories()) {
            var trackRecord = new TrackRecord();
            trackRecord.inventory = inventory;
            trackRecord.currentTime = 0;
            trackRecord.latestAiredCommercialsGroup = -1;
            trackRecord.isAnyAssigned = false;
            trackRecord.isCommercialWithLastAttentionAssigned = false;
            this.trackRecordMap.put(inventory, trackRecord);
        }

        solve();
    }

    private void solve() {
        while (!this.unassignedCommercials.isEmpty()) {
            var commercialAndInventory = selectCommercialAndInventory();
            if (commercialAndInventory == null) {
                break;
            }

            var commercial = commercialAndInventory.first;
            var inventory = commercialAndInventory.second;

            this.updateTrackRecord(inventory, commercial);
            this.updateSolutionMap(inventory, commercial);
            this.updateTotalHourlyCommercialTime(inventory.getHour(), commercial.getDuration());
            this.unassignedCommercials.remove(commercial);
        }

        var _solutionList = new ArrayList<List<SolutionData>>();

        for (var i = 0; i < solutionList.size(); i++) {
            var commercialList = solutionList.get(i);

            var solutionDataList = new ArrayList<SolutionData>();
            for (Commercial commercial : commercialList) {
                solutionDataList.add(new SolutionData(commercial, parameters.getSetOfInventories().get(i)));
            }
            _solutionList.add(i, solutionDataList);
        }

        this.solution = new Solution(_solutionList);
    }

    private Triple<Commercial, Inventory, Double> selectCommercialAndInventory() {
        var rcl = getRCL();
        if (rcl == null) return null;

//        return rcl.get(0);
        return rcl.get(this.random.nextInt(rcl.size()));
    }

    private void updateSolutionMap(Inventory inventory, Commercial commercial) {
        this.solutionList.get(inventory.getId()).add(commercial);
    }

    private void updateTrackRecord(Inventory inventory, Commercial commercial) {
        var trackRecord = this.trackRecordMap.get(inventory);
        trackRecord.currentTime += commercial.getDuration();
        trackRecord.latestAiredCommercialsGroup = commercial.getGroup();
        trackRecord.isAnyAssigned = true;
        trackRecord.isCommercialWithLastAttentionAssigned = commercial.getAttentionMap().get(inventory) == ATTENTION.LAST;
    }

    private List<Triple<Commercial, Inventory, Double>> getRCL() {
        var triples = new ArrayList<Triple<Commercial, Inventory, Double>>(unassignedCommercials.size());

        for (var commercial : this.unassignedCommercials) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                var attention = commercial.getAttentionMapArray()[inventory.getId()];
                if (!checkFeasibility(this.trackRecordMap.get(inventory), commercial, attention)) {
                    continue;
                }

                var greedyScore = calculateGreedyScore(this.trackRecordMap.get(inventory), commercial, attention);
                var newTriple = new Triple<>(commercial, inventory, greedyScore);
                triples.add(newTriple);
            }
        }

        if (triples.isEmpty()) {
            return null;
        }

        var bestScore = triples.stream().map(triple -> triple.third).max(Comparator.naturalOrder()).orElse(0.0);
        var worstScore = triples.stream().map(triple -> triple.third).min(Comparator.naturalOrder()).orElse(0.0);
        var threshold = bestScore - this.alpha * (bestScore - worstScore);

        return triples.stream()
                .filter(triple -> triple.third >= threshold)
                .toList();
    }

    private double calculateGreedyScore(TrackRecord trackRecord, Commercial commercial, ATTENTION attention) {
//        if (attention == ATTENTION.LAST){
//            var remainingInvTime = trackRecord.inventory.getDuration() - trackRecord.currentTime;
//            var remainingHourTime = 720 - getTotalHourlyCommercialTime(trackRecord.inventory.getHour());
//            var remainingTime = Math.min(remainingInvTime, remainingHourTime);
//
//            if (remainingTime > commercial.getDuration() * 2) {
//                return 0;
//            }
//        }

        var minute = (trackRecord.currentTime / 60) + 1;

        var rating = trackRecord.inventory.arrayRatings[minute][commercial.getAudienceType()];
        var score = commercial.getRevenue(rating) / commercial.getDuration();

        if (attention == ATTENTION.FIRST) {
            score *= firstAttentionBoost;
        } else if (attention == ATTENTION.LAST) {
            score *= lastAttentionBoost;
        } else if (attention == ATTENTION.F30) {
            score *= f30AttentionBoost;
        } else if (attention == ATTENTION.F60) {
            score *= f60AttentionBoost;
        }

        return score;
    }

    private boolean checkFeasibility(TrackRecord trackRecord, Commercial commercial, ATTENTION attention) {
        return !trackRecord.isCommercialWithLastAttentionAssigned &&
                checkIfInventoryDurationExceeded(trackRecord.inventory, commercial) &&
                checkIfHourlyLimitExceeded(trackRecord.inventory.getHour(), commercial) &&
                checkIfConsequentSameGroup(trackRecord, commercial) &&
                checkAttentions(trackRecord, attention);
    }

    private boolean checkIfInventoryDurationExceeded(Inventory inventory, Commercial commercial) {
        return this.trackRecordMap.get(inventory).currentTime + commercial.getDuration() <= inventory.getDuration();
    }

    private boolean checkIfHourlyLimitExceeded(int hour, Commercial commercial) {
        var totalCommercialTime = this.getTotalHourlyCommercialTime(hour);

        return totalCommercialTime + commercial.getDuration() <= 720;
    }

    private boolean checkIfConsequentSameGroup(TrackRecord trackRecord, Commercial commercial) {
        return !(trackRecord.latestAiredCommercialsGroup == commercial.getGroup());
    }

    private boolean checkAttentions(TrackRecord trackRecord, ATTENTION attention) {
        if (attention == ATTENTION.NONE) {
            return true;
        } else if (attention == ATTENTION.FIRST) {
            return !trackRecord.isAnyAssigned;
        } else if (attention == ATTENTION.LAST) {
            return true;
        } else if (attention == ATTENTION.F30) {
            return trackRecord.currentTime <= 30;
        } else if (attention == ATTENTION.F60) {
            return trackRecord.currentTime <= 60;
        }

        throw new IllegalArgumentException("Invalid attention type");
    }

    private int getTotalHourlyCommercialTime(int hour) {
        return this.totalCommercialTimes[hour - this.smallestHour];
    }

    private void updateTotalHourlyCommercialTime(int hour, int duration) {
        this.totalCommercialTimes[hour - this.smallestHour] += duration;
    }

    @Override
    public Solution getSolution() {
        return this.solution;
    }
}
