package grasp.reactiveGrasp;

public interface AlphaGenerator {
    double generateAlpha();
    String getStringIdentifier();
    @Override
    int hashCode();
}
