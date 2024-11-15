package runParameters;

public class PathRelinkingSettings {
    private double coeff;

    public PathRelinkingSettings(double coeff) {
        this.coeff = coeff;
    }

    public double getCoeff() {
        return coeff;
    }

    public int getHash() {
        return Double.hashCode(coeff);
    }

    public String stringIdentifier() {
        return "PR" + coeff;
    }
}
