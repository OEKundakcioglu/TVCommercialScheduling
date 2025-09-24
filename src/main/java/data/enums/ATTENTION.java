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
            case "F" -> FIRST;
            case "L" -> LAST;
            case "H" -> F30;
            case "M" -> F60;
            default -> throw new IllegalArgumentException("Unknown attention type: " + name);
        };
    }

    public static String toString(ATTENTION attention) {
        return switch (attention) {
            case NONE -> "NONE";
            case FIRST -> "F";
            case LAST -> "L";
            case F30 -> "H";
            case F60 -> "M";
        };
    }
}
