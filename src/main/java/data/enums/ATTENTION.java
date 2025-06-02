package data.enums;


public enum ATTENTION {
    NONE,
    FIRST,
    LAST,
    F30,
    F60;

    public static ATTENTION fromString(String name) {
        return switch (name) {
            case "NONE" -> NONE;
            case "FIRST" -> FIRST;
            case "LAST" -> LAST;
            case "F30" -> F30;
            case "F60" -> F60;
            default -> throw new IllegalArgumentException("Unknown attention type: " + name);
        };
    }

    public static String toString(ATTENTION attention) {
        return switch (attention) {
            case NONE -> "NONE";
            case FIRST -> "FIRST";
            case LAST -> "LAST";
            case F30 -> "F30";
            case F60 -> "F60";
        };
    }
}
