package data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.enums.ATTENTION;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Utils {

    public static void feasibilityCheck(Solution _solutions) throws Exception {

        checkRemovalAnAdd(_solutions);
        var feasibilityDatas = new LinkedList<FeasibilityData>();
        for (var solutionDataList : _solutions.solution) {
            for (var solutionData : solutionDataList) {
                var isLast = solutionDataList.indexOf(solutionData) == solutionDataList.size() - 1;
                var order = solutionDataList.indexOf(solutionData);
                feasibilityDatas.add(new FeasibilityData(solutionData, isLast, order));
            }
        }

        for (FeasibilityData solution : feasibilityDatas) {
            doesPassAttention(solution);
        }

        if (!doesSatisfyGroupConstraints(_solutions)) {
            throw new IllegalArgumentException("Group constraints are not satisfied!");
        }

        if (!doesSatisfyHourlyDurationRegulation(feasibilityDatas)) {
            throw new IllegalArgumentException("Hourly duration regulation is not satisfied!");
        }

        if (!doesSatisfyInventoryDurations(feasibilityDatas)) {
            throw new IllegalArgumentException("Inventory duration regulation is not satisfied!");
        }

        doesSatisfyEachCommercialAiredAtMostOnce(_solutions);

//        checkRevenues(_solutions);
    }

    private static boolean doesSatisfyGroupConstraints(Solution solution) {
        for (var solutionDataList : solution.solution) {
            if (solutionDataList.isEmpty()) continue;

            SolutionData prevSolutionData = null;
            for (var i = 0; i < solutionDataList.size() - 1; i++) {
                var solutionData = solutionDataList.get(i);

                if (!isGroupConstSatisfied(prevSolutionData, solutionData))
                    throw new IllegalArgumentException("Group constraint is not satisfied");

                prevSolutionData = solutionData;
            }

            var lastSolutionData = solutionDataList.getLast();

            if (!isGroupConstSatisfied(prevSolutionData, lastSolutionData))
                throw new IllegalArgumentException("Group constraint is not satisfied");
        }

        return true;
    }

    private static boolean doesSatisfyHourlyDurationRegulation(List<FeasibilityData> feasibilityDataList) {
        Map<Integer, Double> hourlyDurations = new HashMap<>();
        for (var feasibilityData : feasibilityDataList) {
            var hour = feasibilityData.solutionData().getInventory().getHour();
            Commercial commercial = feasibilityData.solutionData().getCommercial();
            if (!hourlyDurations.containsKey(hour)) {
                hourlyDurations.put(hour, 0.);
            }

            double newDuration = hourlyDurations.get(hour) + commercial.getDuration();
            if (newDuration > 720) {
                return false;
            } else {
                hourlyDurations.replace(hour, newDuration);
            }
        }

        return true;
    }

    private static boolean doesSatisfyInventoryDurations(List<FeasibilityData> feasibilityDataList) {
        Map<Inventory, Double> inventoryDurations = new HashMap<>();
        for (var solution : feasibilityDataList) {
            Commercial commercial = solution.solutionData().getCommercial();
            Inventory inventory = solution.solutionData().getInventory();
            if (!inventoryDurations.containsKey(inventory)) {
                inventoryDurations.put(inventory, 0.);
            }

            double newDuration = inventoryDurations.get(inventory) + commercial.getDuration();
            if (newDuration > inventory.getDuration()) {
                return false;
            } else {
                inventoryDurations.replace(inventory, newDuration);
            }
        }

        return true;
    }

    private static void doesPassAttention(FeasibilityData feasibilityData) throws Exception {
        Commercial commercial = feasibilityData.solutionData().getCommercial();
        Inventory inventory = feasibilityData.solutionData().getInventory();

        ATTENTION attention = commercial.getAttentionMap().get(inventory);
        if (attention == ATTENTION.NONE) {
            //noinspection UnnecessaryReturnStatement
            return;
        } else if (attention == ATTENTION.FIRST) {
            if (feasibilityData.order() == 0) return;
            throw new Exception(String.format("Commercial: %d, Inventory: %d | First attention is not satisfied",
                    commercial.getId(), inventory.getId()));
        } else if (attention == ATTENTION.LAST) {
            if (feasibilityData.isLast()) return;
            throw new Exception(String.format("Commercial: %d, Inventory: %d | Last attention is not satisfied",
                    commercial.getId(), inventory.getId()));
        } else if (attention == ATTENTION.F30) {
            if (feasibilityData.solutionData().getStartTime() <= 30) return;
            throw new Exception(String.format("Commercial: %d, Inventory: %d | F30 attention is not satisfied. It is broadcasted at %f",
                    commercial.getId(), inventory.getId(), feasibilityData.solutionData().getStartTime()));
        } else if (attention == ATTENTION.F60) {
            if (feasibilityData.solutionData().getStartTime() <= 60) return;
            throw new Exception(String.format("Commercial: %d, Inventory: %d | F60 attention is not satisfied. It is broadcasted at %f",
                    commercial.getId(), inventory.getId(), feasibilityData.solutionData().getStartTime()));
        } else {
            throw new IllegalArgumentException("Attention is not valid");
        }
    }

    private static void doesSatisfyEachCommercialAiredAtMostOnce(Solution solution) {
        var occurrenceMap = new HashSet<Commercial>();

        for (var solDataList : solution.solution) {
            for (var solutionData : solDataList) {
                if (occurrenceMap.contains(solutionData.getCommercial())) {
                    throw new IllegalArgumentException("Commercial is aired more than once");
                }
                occurrenceMap.add(solutionData.getCommercial());
            }
        }
    }

    public static void checkRemovalAnAdd(Solution solution) {
        var sumId = Arrays.stream(solution.getSortedSolutionData()).toList()
                .stream()
                .filter(Objects::nonNull)
                .mapToInt(i -> i.getCommercial().getId()).sum();

        var sumId2 = solution.solution.stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .mapToInt(i -> i.getCommercial().getId())
                .sum();

        if (sumId != sumId2) {
            throw new IllegalArgumentException("Removal and add is not correct");
        }

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isGroupConstSatisfied(SolutionData prevSolutionData, SolutionData solutionData) {
        if (prevSolutionData == null) return true;

        return !(prevSolutionData.getCommercial().getGroup() == solutionData.getCommercial().getGroup());
    }

    public static double[] getHourlyDurations(Solution solution, ProblemParameters parameters) {
        var hourlyDurations = new double[parameters.getSetOfHours().size() + 1];
        for (var solutionDataList : solution.solution) {
            if (solutionDataList.isEmpty()) continue;
            var inventory = solutionDataList.getFirst().getInventory();
            for (var solutionData : solutionDataList) {
                hourlyDurations[inventory.getHour()] += solutionData.getCommercial().getDuration();
            }
        }

        return hourlyDurations;
    }

    public static void writeObjectToJson(Object obj, String path) throws IOException {
        var _path = Paths.get(path);
        try{
            var directory = _path.getParent();
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        }
        catch (Exception ignored) {}

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(obj);

        FileWriter writer = new FileWriter(path);
        writer.write(json);
        writer.close();
    }

}
