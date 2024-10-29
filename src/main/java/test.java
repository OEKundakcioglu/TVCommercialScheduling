public class test {
    public static int[] insert(int[] items, int i, int j) {
        if (i < 0 || i >= items.length || j < 0 || j >= items.length) {
            throw new IllegalArgumentException("Invalid indices");
        }

        int[] result = new int[items.length];
        int currentIndex = 0;
        int temp = items[i];

        for (int k = 0; k < items.length; k++) {
            if (k == i) {
                continue;
            }
            if (currentIndex == j-1) {
                result[currentIndex++] = temp;
            }
            result[currentIndex++] = items[k];
        }

        return result;
    }

    private static int[] inversion(int[] items, int low, int high){
        int[] result = new int[items.length];
        for (int i = 0; i < low; i++) {
            result[i] = items[i];
        }
        for (int i = low, j = high; i <= high; i++, j--) {
            result[i] = items[j];
        }
        for (int i = high + 1; i < items.length; i++) {
            result[i] = items[i];
        }
        return result;
    }

    public static void main(String[] args) {
        int[] items = {0, 1, 2, 3, 4};
        int i = 3;
        int j = 4;

        int[] result = inversion(items, i, j);

        System.out.print("Result: ");
        for (int item : result) {
            System.out.print(item + " ");
        }
    }

}
