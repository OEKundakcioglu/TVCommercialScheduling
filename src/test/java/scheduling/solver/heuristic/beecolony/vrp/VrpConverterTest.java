package scheduling.solver.heuristic.beecolony.vrp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;

class VrpConverterTest {

    @Test
    void convertsBasicProblem() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 2.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}, {{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertEquals(2, vrp.getCustomers().length);
        assertEquals(1, vrp.getVehicles().length);
        assertEquals(2, vrp.getDepot().id());

        assertEquals(0, vrp.getCustomers()[0].id());
        assertEquals(10, vrp.getCustomers()[0].serviceTime());
        assertEquals(1, vrp.getCustomers()[0].group());

        assertEquals(1, vrp.getCustomers()[1].id());
        assertEquals(20, vrp.getCustomers()[1].serviceTime());
        assertEquals(2, vrp.getCustomers()[1].group());

        assertEquals(0, vrp.getVehicles()[0].id());
        assertEquals(100, vrp.getVehicles()[0].timeLimit());
        assertEquals(8, vrp.getVehicles()[0].hour());
    }

    @Test
    void distanceIsFeasibleBetweenCompatibleCustomers() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 2.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}, {{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertEquals(0.0, vrp.getDistance()[0][0][1]);
        assertEquals(0.0, vrp.getDistance()[0][1][0]);
    }

    @Test
    void distanceIsInfiniteForSameGroup() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 1, 0, 20, 2.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}, {{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertTrue(Double.isInfinite(vrp.getDistance()[0][0][1]));
        assertTrue(Double.isInfinite(vrp.getDistance()[0][1][0]));
    }

    @Test
    void distanceIsInfiniteWhenNotSuitable() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 2.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {false}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}, {{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertTrue(Double.isInfinite(vrp.getDistance()[0][0][1]));
        assertTrue(Double.isInfinite(vrp.getDistance()[0][1][0]));
    }

    @Test
    void distanceIsInfiniteWhenFromHasLTypeAttention() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 2.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.L1}}, {{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}, {{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertTrue(Double.isInfinite(vrp.getDistance()[0][0][1]));
        assertEquals(0.0, vrp.getDistance()[0][1][0]);
    }

    @Test
    void distanceIsInfiniteWhenToHasF1Attention() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var comm1 = new Commercial(1, 2, 0, 20, 2.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0, comm1};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.F1}}};
        var revenueMatrix = new double[][][] {{{0.0}}, {{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertTrue(Double.isInfinite(vrp.getDistance()[0][0][1]));
        assertEquals(0.0, vrp.getDistance()[0][1][0]);
    }

    @Test
    void depotDistanceIsFeasibleForSuitableCustomer() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);
        var depotId = vrp.getDepot().id();

        assertEquals(0.0, vrp.getDistance()[0][depotId][0]);
        assertEquals(0.0, vrp.getDistance()[0][0][depotId]);
        assertEquals(0.0, vrp.getDistance()[0][depotId][depotId]);
    }

    @Test
    void selfLoopIsInfinite() {
        var comm0 = new Commercial(0, 1, 0, 10, 1.0, PricingType.FIXED);
        var inv0 = new Inventory(0, 100, 8, 10);
        var commercials = new Commercial[] {comm0};
        var inventories = new Inventory[] {inv0};
        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var revenueMatrix = new double[][][] {{{0.0}}};
        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);

        var vrp = VrpConverter.convert(problem);

        assertTrue(Double.isInfinite(vrp.getDistance()[0][0][0]));
    }

    private Problem buildProblem(
            Commercial[] commercials,
            Inventory[] inventories,
            boolean[][] suitability,
            AttentionType[][][] attentionTypes,
            double[][][] revenueMatrix) {
        var hours = Arrays.stream(inventories).mapToInt(Inventory::getHour).distinct().toArray();
        var suitInvFor = new int[commercials.length][];
        for (int c = 0; c < commercials.length; c++) {
            var list = new ArrayList<Integer>();
            for (int i = 0; i < inventories.length; i++) {
                if (suitability[c][i]) {
                    list.add(i);
                }
            }
            suitInvFor[c] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        var suitCommFor = new int[inventories.length][];
        for (int i = 0; i < inventories.length; i++) {
            var list = new ArrayList<Integer>();
            for (int c = 0; c < commercials.length; c++) {
                if (suitability[c][i]) {
                    list.add(c);
                }
            }
            suitCommFor[i] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        return new Problem(
                commercials,
                inventories,
                hours,
                suitability,
                attentionTypes,
                suitInvFor,
                suitCommFor,
                new double[][][] {{{0.0}}},
                revenueMatrix);
    }
}
