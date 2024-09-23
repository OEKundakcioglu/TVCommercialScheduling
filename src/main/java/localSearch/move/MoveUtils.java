package localSearch.move;

import data.Commercial;
import data.Inventory;
import data.SolutionData;
import data.enums.ATTENTION;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class MoveUtils {

    protected static double calculateRevenueGain(Inventory inventory,
                                                 SolutionData solutionData,
                                                 double oldRevenue,
                                                 double currentTime,
                                                 double oldTime) {

        try {
            int currentMinute = (int) (currentTime / 60) + 1;
            int oldMinute = (int) (oldTime / 60) + 1;
            if (currentMinute == oldMinute) {
                return 0;
            }

//            var rating = inventory.ratings.get(currentMinute).get(solutionData.getCommercial().getAudienceType());
            var rating = inventory.arrayRatings[currentMinute][solutionData.getCommercial().getAudienceType()];

            var newRevenue = solutionData.getCommercial().getRevenue(rating);

            return newRevenue - oldRevenue;
        } catch (Exception e) {
            return -Double.MAX_VALUE;
        }
    }

    protected static boolean isAttentionSatisfied(Commercial commercial, Inventory inventory, int position, double startTime, int lastIndexOfInventory) {
        var attention = commercial.getAttentionMapArray()[inventory.getId()];

        if (attention == ATTENTION.NONE) return true;
        if (attention == ATTENTION.FIRST) return position == 0;
        if (attention == ATTENTION.LAST) return position == lastIndexOfInventory;
        if (attention == ATTENTION.F30) return startTime < 30;
        if (attention == ATTENTION.F60) return startTime < 60;

        throw new IllegalArgumentException("Attention type not found");
    }

    protected static boolean isGroupConstraintsSatisfied(Commercial... commercials) {
        int group = -1;
        Commercial prevCommercial = null;
        for (var commercial : commercials) {
            if (commercial == null) {
                group = -1;
                continue;
            }
            if (prevCommercial == commercial) {
                group = commercial.getGroup();
                continue;
            }


            if (commercial.getGroup() == group) return false;
            group = commercial.getGroup();
            prevCommercial = commercial;
        }

        return true;
    }

    protected static double calculateRevenueChange(SolutionData solutionData, Inventory inventory, double shift) {
        double oldStartTime = solutionData.getStartTime();
        int minute = (int) (oldStartTime / 60) + 1;
        var oldRevenue = solutionData.getRevenue();

        double newStartTime = oldStartTime + shift;
        int newMinute = (int) (newStartTime / 60) + 1;

        if (minute == newMinute) return 0;

        var newRevenue = solutionData.getCommercial().getRevenue(inventory, newStartTime);

        return newRevenue - oldRevenue;
    }

    protected static void updateSolutionData(SolutionData solutionData, Inventory inventory, double shift, int position) {
        var revenueGain = MoveUtils.calculateRevenueChange(
                solutionData,
                inventory,
                shift
        );
        var newRevenue = solutionData.getRevenue() + revenueGain;
        var newStartTime = solutionData.getStartTime() + shift;
        solutionData.update(newRevenue, newStartTime, position);
    }
}
