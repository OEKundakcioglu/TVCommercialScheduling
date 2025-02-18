package runParameters;

public record ConstructiveHeuristicSettings(
        double lowerBound,
        double upperBound

) {

    @SuppressWarnings("unused")
    public String getStringIdentifier(){
        return String.format(
                "lowerBound_%f_upperBound_%f",
                this.lowerBound,
                this.upperBound
        );
    }

    @Override
    public int hashCode() {
        return String.format("%f_%f", lowerBound, upperBound).hashCode();
    }
}
