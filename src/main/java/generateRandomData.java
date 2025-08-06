import org.apache.commons.math3.util.Pair;
import randomProblemGenerator.DistributionsJsonLoader;
import randomProblemGenerator.RandomProblemGenerator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class generateRandomData {
    private static final List<Integer> seeds = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final List<Pair<Integer, Integer>> nInvHourPair =
            List.of(new Pair<>(10, 3), new Pair<>(15, 4), new Pair<>(20, 5));
    private static final List<Double> densities = List.of(0.9, 1.1, 1.3);

    public static void main(String[] args) throws IOException {

        for (var seed : seeds) {
            for (var density : densities) {
                for (var pair : nInvHourPair) {
                    var nInventory = pair.getFirst();
                    var nHours = pair.getSecond();

                    var randomGeneratorConf =
                            new DistributionsJsonLoader(Paths.get("distributions.json"), seed)
                                    .load(nInventory, nHours, density);
                    var problem = new RandomProblemGenerator(randomGeneratorConf).generate();
                    var fileName = getFileName(seed, density, nInventory, nHours);
                    problem.writeToPath(Paths.get("instances", fileName));
                }
            }
        }
    }

    private static String getFileName(int seed, double density, int nInventory, int nHours) {
        String densityStr;
        if (density == 0.9) densityStr = "LOW";
        else if (density == 1.1) densityStr = "MEDIUM";
        else if (density == 1.3) densityStr = "HIGH";
        else throw new IllegalArgumentException("Invalid density value: " + density);

        return String.format(
                "density=%s_nInventory=%d_nHours=%d_seed=%d.json",
                densityStr, nInventory, nHours, seed);
    }
}
