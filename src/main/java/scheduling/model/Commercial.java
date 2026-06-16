package scheduling.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scheduling.model.enums.PricingType;

@Getter
@RequiredArgsConstructor
public class Commercial {

    private final int id;
    private final int group;
    private final int audienceType;
    private final int duration;
    private final double price;
    private final PricingType pricingType;

    public double getRevenue(double rating) {
        return switch (pricingType) {
            case FIXED -> price * duration;
            case PPR -> rating * price * duration;
        };
    }
}
