package scheduling.dto;

import java.util.List;

public record ProblemDTO(
        List<CommercialDTO> commercials, List<InventoryDTO> inventories, List<RatingDTO> ratings) {}
