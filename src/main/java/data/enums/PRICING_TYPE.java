package data.enums;

public enum PRICING_TYPE {
    PRR, FIXED;


    public static PRICING_TYPE fromString(String name) {
        return switch (name) {
            case "CPP" -> PRR;
            case "FixPrice" -> FIXED;
            default -> throw new IllegalArgumentException("Unknown pricing type: " + name);
        };
    }
}
