package data.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import data.Commercial;
import data.enums.ATTENTION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommercialSerializer extends JsonSerializer<Commercial> {
    @Override
    public void serialize(
            Commercial commercial,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("id", commercial.getId());
        jsonGenerator.writeNumberField("group", commercial.getGroup());
        jsonGenerator.writeNumberField("audienceType", commercial.getAudienceType());
        jsonGenerator.writeNumberField("duration", commercial.getDuration());
        jsonGenerator.writeNumberField("price", commercial.getPrice());

        jsonGenerator.writeStringField("pricingType", commercial.getPricingType().name());

        serializeSuitableInventories(commercial, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    private void serializeSuitableInventories(
            Commercial commercial,
            JsonGenerator jsonGenerator)
            throws IOException {
        var suitableInventories = new HashMap<ATTENTION, List<Integer>>();

        var attentionMap = commercial.getAttentionMap();
        for (var entry : attentionMap.entrySet()) {
            var attention = entry.getValue();
            var inventory = entry.getKey();

            suitableInventories
                    .computeIfAbsent(attention, k -> new ArrayList<>())
                    .add(inventory.getId());
        }

        jsonGenerator.writeObjectFieldStart("suitableInventories");
        for (var entry : suitableInventories.entrySet()) {
            var attention = entry.getKey();
            var inventories = entry.getValue();

            jsonGenerator.writeArrayFieldStart(ATTENTION.toString(attention));
            for (var inventoryId : inventories) {
                jsonGenerator.writeNumber(inventoryId);
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }
}
