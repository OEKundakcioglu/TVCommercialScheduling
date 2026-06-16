package scheduling.model.enums;

public enum AttentionType {
    N,
    F1,
    F2,
    F3,
    F12,
    F123,
    L1,
    L2,
    L3,
    L12,
    L123;

    public static boolean anySatisfied(AttentionType[] types, int position, int sequenceLength) {
        if (types.length == 0) {
            return true;
        }
        for (AttentionType type : types) {
            if (type.isSatisfied(position, sequenceLength)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSatisfied(int position, int sequenceLength) {
        return switch (this) {
            case N -> true;
            case F1 -> position == 0;
            case F2 -> position == 1;
            case F3 -> position == 2;
            case F12 -> position <= 1;
            case F123 -> position <= 2;
            case L1 -> position == sequenceLength - 1;
            case L2 -> position == sequenceLength - 2;
            case L3 -> position == sequenceLength - 3;
            case L12 -> position >= sequenceLength - 2;
            case L123 -> position >= sequenceLength - 3;
        };
    }

    public boolean isLType() {
        return switch (this) {
            case L1, L2, L3, L12, L123 -> true;
            default -> false;
        };
    }

    public boolean isFType() {
        return switch (this) {
            case F1, F2, F3, F12, F123 -> true;
            default -> false;
        };
    }

    public int[] getPositions() {
        return switch (this) {
            case N -> new int[0];
            case F1 -> new int[] {1};
            case F2 -> new int[] {2};
            case F3 -> new int[] {3};
            case F12 -> new int[] {1, 2};
            case F123 -> new int[] {1, 2, 3};
            case L1 -> new int[] {1};
            case L2 -> new int[] {2};
            case L3 -> new int[] {3};
            case L12 -> new int[] {1, 2};
            case L123 -> new int[] {1, 2, 3};
        };
    }
}
