package data.enums;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public enum PRICING_TYPE {
    PRR, FIXED;


    public static PRICING_TYPE fromString(String name) {
        return switch (name) {
            case "CPP" -> PRR;
            case "FixPrice" -> FIXED;
            default -> throw new IllegalArgumentException("Unknown pricing type: " + name);
        };
    }

    public static class Serializer extends JsonSerializer<PRICING_TYPE> {

        @Override
        public void serialize(
                PRICING_TYPE pricingType,
                JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider)
                throws IOException {

            var value =
                    switch (pricingType) {
                        case PRR -> "CPP";
                        case FIXED -> "FixPrice";
                    };
            jsonGenerator.writeString(value);
        }
    }
}
