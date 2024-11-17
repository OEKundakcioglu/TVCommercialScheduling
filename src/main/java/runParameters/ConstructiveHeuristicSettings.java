package runParameters;

public record ConstructiveHeuristicSettings(
        double lowerBound,
        double upperBound,
        int lastCoefficient

) {

    @SuppressWarnings("unused")
    public String getStringIdentifier(){
        return String.format(
                "lowerBound_%f_upperBound_%f_lastCoefficient_%d",
                this.lowerBound,
                this.upperBound,
                this.lastCoefficient
        );
    }

    @Override
    public int hashCode() {
        return String.format("%f_%f", lowerBound, upperBound, lastCoefficient).hashCode();
    }
}
