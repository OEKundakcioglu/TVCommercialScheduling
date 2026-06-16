package scheduling.dto;

import java.util.List;

public record AssignmentDTO(int inventoryId, List<Integer> commercialIds) {}
