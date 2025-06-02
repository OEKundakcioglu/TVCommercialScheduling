package data.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;

public class JsonSerializableObject {

    private static final ObjectWriter writer =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writer();

    public void writeToPath(Path path) throws IOException {
        writer.writeValue(path.toFile(), this);
    }
}
