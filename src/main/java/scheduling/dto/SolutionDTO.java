package scheduling.dto;

import java.util.List;

public record SolutionDTO(double totalRevenue, List<AssignmentDTO> assignments) {}
