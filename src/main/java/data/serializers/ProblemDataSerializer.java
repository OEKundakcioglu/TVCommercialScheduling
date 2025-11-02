package data.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import data.ProblemParameters;

import java.io.IOException;

public class ProblemDataSerializer extends JsonSerializer<ProblemParameters> {
    @Override
    public void serialize(
            ProblemParameters value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();

        gen.writeArrayFieldStart("commercials");
        var commercialSerializer = new CommercialSerializer();
        for (var commercial : value.getSetOfCommercials()) {
            commercialSerializer.serialize(commercial, gen, serializers);
        }
        gen.writeEndArray();

        var inventorySerializer = new InventorySerializer();
        gen.writeArrayFieldStart("inventories");
        for (var inventory : value.getSetOfInventories()) {
            inventorySerializer.serialize(inventory, gen, serializers);
        }
        gen.writeEndArray();

        serializeRatings(value, gen);
    }

    private void serializeRatings(
            ProblemParameters problemParameters, JsonGenerator gen)
            throws IOException {
        gen.writeArrayFieldStart("ratings");
        for (var inventory : problemParameters.getRatings().keySet()) {
            for (var minute : problemParameters.getRatings().get(inventory).keySet()) {
                for (var audienceType :
                        problemParameters.getRatings().get(inventory).get(minute).keySet()) {
                    var rating =
                            problemParameters
                                    .getRatings()
                                    .get(inventory)
                                    .get(minute)
                                    .get(audienceType);
                    gen.writeStartObject();
                    gen.writeNumberField("inventoryId", inventory.getId());
                    gen.writeNumberField("minute", minute);
                    gen.writeNumberField("rating", rating);
                    gen.writeNumberField("audienceType", audienceType);
                    gen.writeEndObject();
                }
            }
        }
        gen.writeEndArray();
    }
}
