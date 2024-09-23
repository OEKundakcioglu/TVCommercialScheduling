package data;

public class SolutionData {
    private final transient Commercial commercial;
    private transient Inventory inventory;

    private double revenue;
    private double startTime;
    private double endTime;
    private int position;

    public SolutionData(Commercial commercial, Inventory inventory) {
        this.commercial = commercial;
        this.inventory = inventory;
    }

    public SolutionData copy() {
        SolutionData copy = new SolutionData(commercial, inventory);
        copy.revenue = revenue;
        copy.startTime = startTime;
        copy.endTime = endTime;
        copy.position = position;
        return copy;
    }

    public void update(Inventory inventory, double revenue, double startTime, int position){
        this.inventory = inventory;
        this.revenue = revenue;
        this.startTime = startTime;
        this.position = position;
        this.endTime = startTime + commercial.getDuration();
    }

    public void update(double revenue, double startTime, int position){
        this.revenue = revenue;
        this.startTime = startTime;
        this.position = position;
        this.endTime = startTime + commercial.getDuration();
    }

    public Commercial getCommercial() {
        return commercial;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public double getRevenue() {
        return revenue;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public int getPosition() {
        return position;
    }
}
