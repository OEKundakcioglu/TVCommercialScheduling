package randomProblemGenerator;

import java.util.Map;

public record InventorySample(int duration, Map<Integer, Map<Integer, Double>> ratings) {
}
