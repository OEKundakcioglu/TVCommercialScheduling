package scheduling.solver.heuristic.grasp.construction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import scheduling.model.Commercial;
import scheduling.model.Inventory;
import scheduling.model.Problem;
import scheduling.model.enums.AttentionType;
import scheduling.model.enums.PricingType;
import scheduling.solver.FeasibilityCheck;

class GreedyConstructionTest {

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

    @Test
    void assignsSingleCommercialWithNAttention() {
        var commercials = new Commercial[] {new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED)};
        var inventories = new Inventory[] {new Inventory(0, 100, 0, 10)};
        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var revenueMatrix = new double[][][] {new double[][] {new double[100]}};
        Arrays.fill(revenueMatrix[0][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var solver = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01);
        var result = solver.solve();

        assertArrayEquals(new int[] {0}, result.getSequences()[0]);
        assertArrayEquals(new int[] {0}, result.getStartTimes()[0]);
        assertEquals(50.0, result.getRevenues()[0][0], 1e-9);
        assertEquals(50.0, result.getTotalRevenue(), 1e-9);
        assertEquals(10, result.getTotalInvDuration()[0]);
        assertEquals(10, result.getTotalDurationOfHour()[0]);
    }

    @Test
    void rejectsAppendWhenL1CommercialWouldBeViolated() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 100, 0, 10)};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.L1}}, {{AttentionType.N}}};
        var revenueMatrix =
                new double[][][] {
                    new double[][] {new double[100]}, new double[][] {new double[100]}
                };
        Arrays.fill(revenueMatrix[0][0], 50.0);
        Arrays.fill(revenueMatrix[1][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        // comm0 with L1 is placed (position 0, length 1 satisfies L1), then inventory is frozen
        assertEquals(1, result.getSequences()[0].length);
    }

    @Test
    void respectsDurationConstraint() {
        var commercials = new Commercial[] {new Commercial(0, 0, 0, 200, 5.0, PricingType.FIXED)};
        var inventories = new Inventory[] {new Inventory(0, 100, 0, 10)};
        var suitability = new boolean[][] {{true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}};
        var revenueMatrix = new double[][][] {new double[][] {new double[100]}};

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        assertEquals(0, result.getSequences()[0].length);
        assertEquals(0.0, result.getTotalRevenue(), 1e-9);
    }

    @Test
    void respectsGroupConstraint() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 1, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 100, 0, 10)};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.N}}};
        var revenueMatrix =
                new double[][][] {
                    new double[][] {new double[100]}, new double[][] {new double[100]}
                };
        Arrays.fill(revenueMatrix[0][0], 50.0);
        Arrays.fill(revenueMatrix[1][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        assertEquals(1, result.getSequences()[0].length);
    }

    @Test
    void respectsFTypeAttention() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 10.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 100, 0, 10)};
        var suitability = new boolean[][] {{true}, {true}};
        var attentionTypes = new AttentionType[][][] {{{AttentionType.N}}, {{AttentionType.F1}}};
        var revenueMatrix =
                new double[][][] {
                    new double[][] {new double[100]}, new double[][] {new double[100]}
                };
        Arrays.fill(revenueMatrix[0][0], 100.0);
        Arrays.fill(revenueMatrix[1][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        assertEquals(1, result.getSequences()[0].length);
        assertEquals(0, result.getSequences()[0][0]);
    }

    @Test
    void respectsMaxCommercialCount() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(2, 2, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 100, 0, 2)};
        var suitability = new boolean[][] {{true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}}, {{AttentionType.N}}, {{AttentionType.N}}
                };
        var revenueMatrix =
                new double[][][] {
                    new double[][] {new double[100]},
                    new double[][] {new double[100]},
                    new double[][] {new double[100]}
                };
        Arrays.fill(revenueMatrix[0][0], 50.0);
        Arrays.fill(revenueMatrix[1][0], 50.0);
        Arrays.fill(revenueMatrix[2][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        assertEquals(2, result.getSequences()[0].length);
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void respectsHourlyLimit() {
        var commercials = new Commercial[4];
        var suitability = new boolean[4][1];
        var attentionTypes = new AttentionType[4][1][];
        var revenueMatrix = new double[4][1][];
        for (int i = 0; i < 4; i++) {
            commercials[i] = new Commercial(i, i, 0, 200, 5.0, PricingType.FIXED);
            suitability[i][0] = true;
            attentionTypes[i][0] = new AttentionType[] {AttentionType.N};
            revenueMatrix[i][0] = new double[3600];
            Arrays.fill(revenueMatrix[i][0], 1000.0);
        }
        var inventories = new Inventory[] {new Inventory(0, 3600, 0, 10)};

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        assertEquals(3, result.getSequences()[0].length);
        assertEquals(600, result.getTotalDurationOfHour()[0]);
    }

    @Test
    void integrationTestPassesFeasibilityCheck() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 15, 3.0, PricingType.FIXED),
                    new Commercial(2, 0, 0, 20, 8.0, PricingType.PPR),
                    new Commercial(3, 2, 0, 10, 4.0, PricingType.FIXED),
                    new Commercial(4, 1, 0, 25, 6.0, PricingType.FIXED),
                    new Commercial(5, 3, 0, 10, 2.0, PricingType.FIXED)
                };
        var inventories =
                new Inventory[] {new Inventory(0, 200, 0, 10), new Inventory(1, 150, 1, 10)};
        var suitability =
                new boolean[][] {
                    {true, true},
                    {true, false},
                    {false, true},
                    {true, true},
                    {true, false},
                    {true, true}
                };
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}, {AttentionType.F1}},
                    {{AttentionType.N}, {}},
                    {{}, {AttentionType.N}},
                    {{AttentionType.N}, {AttentionType.N}},
                    {{AttentionType.N}, {}},
                    {{AttentionType.N}, {AttentionType.N}}
                };
        var revenueMatrix = new double[6][2][];
        for (int c = 0; c < 6; c++) {
            for (int i = 0; i < 2; i++) {
                if (suitability[c][i]) {
                    revenueMatrix[c][i] = new double[inventories[i].getDuration()];
                    Arrays.fill(revenueMatrix[c][i], (c + 1) * 10.0);
                } else {
                    revenueMatrix[c][i] = new double[0];
                }
            }
        }

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.5, new Random(42), 0.8, 1.2).solve();

        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void multipleInventoriesAssignCommercials() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(2, 2, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories =
                new Inventory[] {new Inventory(0, 100, 0, 10), new Inventory(1, 100, 0, 10)};
        var suitability = new boolean[][] {{true, false}, {false, true}, {true, false}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.N}, {}}, {{}, {AttentionType.N}}, {{AttentionType.N}, {}}
                };
        var revenueMatrix =
                new double[][][] {
                    {new double[100], new double[0]},
                    {new double[0], new double[100]},
                    {new double[100], new double[0]}
                };
        Arrays.fill(revenueMatrix[0][0], 50.0);
        Arrays.fill(revenueMatrix[1][1], 50.0);
        Arrays.fill(revenueMatrix[2][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        assertEquals(2, result.getSequences()[0].length);
        assertEquals(1, result.getSequences()[1].length);
        assertEquals(1, result.getSequences()[1][0]);
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void preventsL12ViolationOfExistingCommercial() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(2, 2, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(3, 3, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 200, 0, 10)};
        var suitability = new boolean[][] {{true}, {true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.L12}},
                    {{AttentionType.N}},
                    {{AttentionType.N}},
                    {{AttentionType.N}}
                };
        var revenueMatrix =
                new double[][][] {
                    new double[][] {new double[200]},
                    new double[][] {new double[200]},
                    new double[][] {new double[200]},
                    new double[][] {new double[200]}
                };
        Arrays.fill(revenueMatrix[0][0], 100.0);
        Arrays.fill(revenueMatrix[1][0], 50.0);
        Arrays.fill(revenueMatrix[2][0], 50.0);
        Arrays.fill(revenueMatrix[3][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        // Comm 0 (L12) placed at pos 0, comm 1 placed at pos 1 (seqLen=2).
        // A third append would make seqLen=3, violating L12 for comm 0 (0 >= 3-2=1 fails).
        assertEquals(2, result.getSequences()[0].length);
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }

    @Test
    void allowsAppendWhenBroaderLTypeFlagRemainsSatisfied() {
        var commercials =
                new Commercial[] {
                    new Commercial(0, 0, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(1, 1, 0, 10, 5.0, PricingType.FIXED),
                    new Commercial(2, 2, 0, 10, 5.0, PricingType.FIXED)
                };
        var inventories = new Inventory[] {new Inventory(0, 200, 0, 10)};
        var suitability = new boolean[][] {{true}, {true}, {true}};
        var attentionTypes =
                new AttentionType[][][] {
                    {{AttentionType.L1, AttentionType.L12}},
                    {{AttentionType.N}},
                    {{AttentionType.N}}
                };
        var revenueMatrix =
                new double[][][] {
                    new double[][] {new double[200]},
                    new double[][] {new double[200]},
                    new double[][] {new double[200]}
                };
        Arrays.fill(revenueMatrix[0][0], 100.0);
        Arrays.fill(revenueMatrix[1][0], 50.0);
        Arrays.fill(revenueMatrix[2][0], 50.0);

        var problem =
                buildProblem(commercials, inventories, suitability, attentionTypes, revenueMatrix);
        var result = new GreedyConstruction(problem, 0.0, new Random(42), 0.99, 1.01).solve();

        // Comm 0 ([L1, L12]) at pos 0. With seqLen=2, L12 (0 >= 0) still holds, so comm 1 allowed.
        // With seqLen=3, L12 (0 >= 1) fails, so comm 2 blocked. Result: 2 commercials, not 1.
        assertEquals(2, result.getSequences()[0].length);
        assertDoesNotThrow(() -> FeasibilityCheck.check(problem, result));
    }
}
