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

    private final ConstructiveHeuristicSettings constructiveHeuristicSettings;

    public ConstructiveHeuristic(
            ProblemParameters parameters,
            double alpha,
            Random random,
            ConstructiveHeuristicSettings constructiveHeuristicSettings) {
        this.random = random;
        this.alpha = alpha;

        this.constructiveHeuristicSettings = constructiveHeuristicSettings;

        this.firstAttentionBoost =
                random.nextDouble(
                        constructiveHeuristicSettings.lowerBound(),
                        constructiveHeuristicSettings.upperBound());
        this.lastAttentionBoost =
                random.nextDouble(
                        constructiveHeuristicSettings.lowerBound(),
                        constructiveHeuristicSettings.upperBound());
        this.f30AttentionBoost =
                random.nextDouble(
                        constructiveHeuristicSettings.lowerBound(),
                        constructiveHeuristicSettings.upperBound());
        this.f60AttentionBoost =
                random.nextDouble(
                        constructiveHeuristicSettings.lowerBound(),
                        constructiveHeuristicSettings.upperBound());

        this.parameters = parameters;
        this.unassignedCommercials = new ArrayList<>(this.parameters.getSetOfCommercials());
        this.totalCommercialTimes = new int[this.parameters.getSetOfHours().size()];
        this.smallestHour =
                this.parameters.getSetOfHours().stream()
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElse(0);

        this.solutionList = new ArrayList<>();

        this.parameters
                .getSetOfInventories()
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
                solutionDataList.add(
                        new SolutionData(commercial, parameters.getSetOfInventories().get(i)));
            }
            _solutionList.add(i, solutionDataList);
        }

        this.solution = new Solution(_solutionList);
    }

    private void updateSolutionMap(Inventory inventory, Commercial commercial) {
        this.solutionList.get(inventory.getId()).add(commercial);
    }

    private void updateTrackRecord(Inventory inventory, Commercial commercial) {
        var trackRecord = this.trackRecordMap.get(inventory);
        trackRecord.currentTime += commercial.getDuration();
        trackRecord.latestAiredCommercialsGroup = commercial.getGroup();
        trackRecord.isAnyAssigned = true;
        trackRecord.isCommercialWithLastAttentionAssigned =
                commercial.getAttentionMap().get(inventory) == ATTENTION.LAST;
    }

    private Triple<Commercial, Inventory, Double> selectCommercialAndInventory() {
        var commercials = new ArrayList<Commercial>();
        var inventories = new ArrayList<Inventory>();
        var scores = new ArrayList<Double>();

        var worstScore = Double.POSITIVE_INFINITY;

        var bestScore = Double.NEGATIVE_INFINITY;

        for (var commercial : this.unassignedCommercials) {
            for (var inventory : commercial.getSetOfSuitableInv()) {
                var attention = commercial.getAttentionMapArray()[inventory.getId()];
                if (!checkFeasibility(this.trackRecordMap.get(inventory), commercial, attention)) {
                    continue;
                }

                var greedyScore =
                        calculateGreedyScore(
                                this.trackRecordMap.get(inventory), commercial, attention);

                commercials.add(commercial);
                inventories.add(inventory);
                scores.add(greedyScore);

                if (greedyScore < worstScore) {
                    worstScore = greedyScore;
                }

                if (greedyScore > bestScore) {
                    bestScore = greedyScore;
                }
            }
        }

        if (commercials.isEmpty()) {
            return null;
        }

        var threshold = bestScore - this.alpha * (bestScore - worstScore);
        return getReservoirSample(commercials, inventories, scores, threshold);
    }

    private Triple<Commercial, Inventory, Double> getReservoirSample(
            List<Commercial> commercials,
            List<Inventory> inventories,
            List<Double> scores,
            double threshold) {
        Commercial selectedCommercial = null;
        Inventory selectedInventory = null;
        double selectedScore = 0.0;

        int count = 0;
        for (int i = 0; i < commercials.size(); i++) {
            if (scores.get(i) >= threshold) {
                count++;
                if (random.nextInt(count) == 0) {
                    selectedCommercial = commercials.get(i);
                    selectedInventory = inventories.get(i);
                    selectedScore = scores.get(i);
                }
            }
        }

        return new Triple<>(selectedCommercial, selectedInventory, selectedScore);
    }

    private double calculateGreedyScore(
            TrackRecord trackRecord, Commercial commercial, ATTENTION attention) {

        if (attention == ATTENTION.LAST) {
            int remainingTime = trackRecord.inventory.getDuration() - trackRecord.currentTime;
            if (remainingTime > commercial.getDuration() * constructiveHeuristicSettings.lastCoefficient()) {
                return 0;
            }
        }

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

    private boolean checkFeasibility(
            TrackRecord trackRecord, Commercial commercial, ATTENTION attention) {
        return !trackRecord.isCommercialWithLastAttentionAssigned
                && checkIfInventoryDurationExceeded(trackRecord.inventory, commercial)
                && checkIfHourlyLimitExceeded(trackRecord.inventory.getHour(), commercial)
                && checkIfConsequentSameGroup(trackRecord, commercial)
                && checkAttentions(trackRecord, attention);
    }

    private boolean checkIfInventoryDurationExceeded(Inventory inventory, Commercial commercial) {
        return this.trackRecordMap.get(inventory).currentTime + commercial.getDuration()
                <= inventory.getDuration();
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
