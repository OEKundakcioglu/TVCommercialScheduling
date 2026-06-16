package scheduling.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import scheduling.dto.ProblemDTO;

public final class ProblemDTOReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProblemDTOReader() {}

    public static ProblemDTO read(Path path) {
        try {
            return MAPPER.readValue(path.toFile(), ProblemDTO.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
