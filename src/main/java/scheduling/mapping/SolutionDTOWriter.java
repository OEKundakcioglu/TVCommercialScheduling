package scheduling.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import scheduling.dto.AssignmentDTO;
import scheduling.dto.CheckPointDTO;
import scheduling.dto.SolutionDTO;
import scheduling.dto.SolverSolutionDTO;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.solver.CheckPoint;
import scheduling.solver.Solution;
import scheduling.solver.SolverSolution;

public final class SolutionDTOWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SolutionDTOWriter() {}

    public static <T> void write(SolverSolution<T> solverSolution, Path path) {
        var dto = toDTO(solverSolution);
        try {
            var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), dto);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> SolverSolutionDTO<T> toDTO(SolverSolution<T> solverSolution) {
        return new SolverSolutionDTO<>(
                toSolutionDTO(solverSolution.getBestSolution()),
                toCheckPointDTOs(solverSolution.getCheckPoints()),
                solverSolution.getAdditionalInformation());
    }

    private static SolutionDTO toSolutionDTO(Solution solution) {
        var assignments =
                solution.getAssignments().entrySet().stream()
                        .map(e -> toAssignmentDTO(e.getKey(), e.getValue()))
                        .toList();
        return new SolutionDTO(solution.getTotalRevenue(), assignments);
    }

    private static AssignmentDTO toAssignmentDTO(
            Inventory inventory, List<Commercial> commercials) {
        var commercialIds = commercials.stream().map(Commercial::getId).toList();
        return new AssignmentDTO(inventory.getId(), commercialIds);
    }

    private static List<CheckPointDTO> toCheckPointDTOs(List<CheckPoint> checkPoints) {
        return checkPoints.stream()
                .map(cp -> new CheckPointDTO(cp.getObjective(), cp.getTime()))
                .toList();
    }
}
