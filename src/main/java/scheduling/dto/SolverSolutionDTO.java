package scheduling.dto;

import java.util.List;

public record SolverSolutionDTO<T>(
        SolutionDTO bestSolution, List<CheckPointDTO> checkPoints, T additionalInformation) {}
