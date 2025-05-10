package runParameters;

import data.enums.ATTENTION;
import data.enums.PRICING_TYPE;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.List;

public record RandomGeneratorConfig (
        int seed,
        int nCommercial,
        int nInventory,
        RealDistribution ratingDistribution,
        RealDistribution inventoryDurationDistribution,
        RealDistribution commercialDurationDistribution,
        RealDistribution pprDistribution,
        RealDistribution fixedDistribution,
        EnumeratedDistribution<PRICING_TYPE> pricingTypeDistribution,
        EnumeratedDistribution<Integer> hourDistribution,
        EnumeratedDistribution<ATTENTION> attentionDistribution,
        EnumeratedDistribution<Integer> audienceTypeDistribution,
        EnumeratedDistribution<Integer> groupDistribution,
        BinomialDistribution suitableInvDistribution
) {

    public List<Integer> audienceTypes() {
        return audienceTypeDistribution.getPmf().stream()
                .map(Pair::getFirst)
                .toList();
    }
}
