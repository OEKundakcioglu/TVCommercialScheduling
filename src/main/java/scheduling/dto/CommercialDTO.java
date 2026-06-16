package scheduling.dto;

import java.util.List;
import java.util.Map;

public record CommercialDTO(
        int id,
        int group,
        int audienceType,
        int duration,
        double price,
        String pricingType,
        Map<String, List<Integer>> suitableInventories) {}
