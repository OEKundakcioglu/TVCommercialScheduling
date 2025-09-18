package runParameters;

public record ConstructiveHeuristicSettings(
        double lowerBound,
        double upperBound

) {

    @SuppressWarnings("unused")
    public String getStringIdentifier(){
        return String.format(
                "lowerBound=%.2f_upperBound=%.2f",
                this.lowerBound,
                this.upperBound
        );
    }
}
