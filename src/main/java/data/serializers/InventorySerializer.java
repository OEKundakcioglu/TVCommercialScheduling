package data.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import data.Inventory;

import java.io.IOException;

public class InventorySerializer extends JsonSerializer<Inventory> {

    @Override
    public void serialize(
            Inventory inventory, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", inventory.getId());
        jsonGenerator.writeNumberField("group", inventory.getDuration());
        jsonGenerator.writeNumberField("duration", inventory.getDuration());
        jsonGenerator.writeNumberField("hour", inventory.getHour());
        jsonGenerator.writeNumberField(
                "max_number_of_commercial", inventory.getMaxCommercialCount());
        jsonGenerator.writeEndObject();
    }
}
