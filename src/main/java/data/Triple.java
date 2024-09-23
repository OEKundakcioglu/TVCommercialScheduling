package data;

public class Triple<T, K, L> {
    public T first;
    public K second;
    public L third;

    public Triple(T first, K second, L third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
}
