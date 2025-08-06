package solvers.heuristicSolvers.beeColonyYu;

import data.ProblemParameters;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import runParameters.ConstructiveHeuristicSettings;
import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.beeColonyYu.localSearch.NeighborhoodFunction;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class BeeColonyAlgorithm {
    private final OrienteeringData orienteeringData;
    private final BeeColonyUtils beeColonyUtils;
    private final Random random;
    private final ProblemParameters parameters;
    private final List<BeeColonySolution> employedBees;
    private final BeeColonySettings beeColonySettings;
    private final List<BeeColonyCheckPoint> checkPoints;
    private final SolverSolution solverSolution;
    private BeeColonySolution bestSolution;
    private double totalFitness = 0;

    public BeeColonyAlgorithm(
            OrienteeringData orienteeringData,
            BeeColonySettings beeColonySettings,
            ProblemParameters parameters) {
        this.orienteeringData = orienteeringData;
        this.beeColonySettings = beeColonySettings;
        this.parameters = parameters;
        this.checkPoints = new ArrayList<>();
        this.random = new Random(beeColonySettings.getSeed());

        this.beeColonyUtils = new BeeColonyUtils(orienteeringData, parameters);

        employedBees = new ArrayList<>();
        initPopulation();
        solve();

        this.solverSolution =
                new SolverSolution(
                        beeColonyUtils.toSolution(bestSolution),
                        checkPoints.stream()
                                .map(
                                        beeColonyCheckPoint ->
                                                new CheckPoint(
                                                        beeColonyUtils.toSolution(
                                                                beeColonyCheckPoint.getSolution()),
                                                        beeColonyCheckPoint.getTime()))
                                .toList(),
                        beeColonySettings,
                        parameters.getInstance());
    }

    private void initPopulation() {
        var allStringValues =
                new ArrayList<>(
                        IntStream.range(0, orienteeringData.getCustomers().size())
                                .boxed()
                                .toList());
        for (var i = 0; i < orienteeringData.getVehicles().size() - 1; i++) {
            allStringValues.add(orienteeringData.getDepot().getId());
        }

        for (var i = 0; i < beeColonySettings.populationSize(); i++) {
            Collections.shuffle(allStringValues);
            //                        int[] solString =
            //             allStringValues.stream().mapToInt(Integer::intValue).toArray();

            var solString = generateRandomSolution();
            var newSolution =
                    new BeeColonySolution(solString, beeColonyUtils.calculateFitness(solString));
            employedBees.add(newSolution);
            totalFitness += newSolution.getFitness();

            if (bestSolution == null || newSolution.getFitness() > bestSolution.getFitness()) {
                bestSolution = newSolution;
            }
        }
    }

    private void solve() {
        var neighborhoodFunction = new NeighborhoodFunction(beeColonyUtils);

        int G = 0;
        double T = beeColonySettings.T0();

        ProgressBar pb =
                new ProgressBarBuilder()
                        .setUnit("s", 1)
                        .setInitialMax(beeColonySettings.timeLimit())
                        .hideEta()
                        .setConsumer(new ConsoleProgressBarConsumer(System.out, 120))
                        .setTaskName("Yu Bee Colony")
                        .build();

        long startTime = System.currentTimeMillis() / 1000;

        long currentTime = System.currentTimeMillis() / 1000;
        while (currentTime - startTime < beeColonySettings.timeLimit()) {
            G++;

            for (var i = 0; i < beeColonySettings.populationSize(); i++) {
                var sol = employedBees.get(i);
                var newSol = neighborhoodFunction.apply(sol);
                double delta = (newSol.getFitness() - sol.getFitness()) / sol.getFitness();

                if (delta >= 0) {
                    employedBees.set(i, newSol);
                    totalFitness += newSol.getFitness();
                    totalFitness -= sol.getFitness();
                    if (newSol.getFitness() > bestSolution.getFitness()) {
                        updateBestSol(
                                newSol, pb, (System.currentTimeMillis() / 1000 - startTime), G);
                    }
                } else {
                    double r = random.nextDouble();
                    if (r < Math.exp(delta / T)) {
                        employedBees.set(i, newSol);
                        totalFitness += newSol.getFitness();
                        totalFitness -= sol.getFitness();
                    }
                }
            }

            for (var i = 0; i < beeColonySettings.populationSize(); i++) {
                var bee = selectEmployedBee();
                var sol = employedBees.get(bee);
                var newSol = neighborhoodFunction.apply(sol);

                double delta = (newSol.getFitness() - sol.getFitness()) / sol.getFitness();
                if (delta >= 0) {
                    employedBees.set(bee, newSol);
                    totalFitness += newSol.getFitness();
                    totalFitness -= sol.getFitness();
                    if (newSol.getFitness() > bestSolution.getFitness()) {
                        updateBestSol(
                                newSol, pb, (System.currentTimeMillis() / 1000 - startTime), G);
                    }
                } else {
                    double r = random.nextDouble();
                    if (r < Math.exp(delta / T)) {
                        employedBees.set(bee, newSol);
                        totalFitness += newSol.getFitness();
                        totalFitness -= sol.getFitness();
                    }
                }
            }

            if (G % beeColonySettings.nIter() == 0) T = beeColonySettings.alpha() * T;

            currentTime = System.currentTimeMillis() / 1000;
            pb.stepTo((int) (currentTime - startTime));
        }

        pb.close();
    }

    private int selectEmployedBee() {
        double summedFitness = totalFitness;
        double r = random.nextDouble() * summedFitness;

        double currentSum = 0;
        for (var i = 0; i < beeColonySettings.populationSize(); i++) {
            currentSum += employedBees.get(i).getFitness();
            if (currentSum >= r) {
                return i;
            }
        }

        throw new IllegalStateException("Should not reach here");
    }

    private void updateBestSol(
            BeeColonySolution solution, ProgressBar pb, long passedTime, int nIter) {
        this.bestSolution = solution;
        this.checkPoints.add(new BeeColonyCheckPoint(solution, passedTime));
        pb.setExtraMessage(
                String.format(
                        "Best solution: %d found at %d seconds | Iteration: %d | Iter/s: %.2f",
                        (int) bestSolution.getFitness(),
                        passedTime,
                        nIter,
                        nIter / (double) passedTime));
    }

    private int[] generateRandomSolution() {
        var constructive =
                new ConstructiveHeuristic(
                        parameters, 0.5, new ConstructiveHeuristicSettings(0.5, 5));
        var sol = constructive.getSolution();

        var solString = new ArrayList<Integer>();
        for (var inventoy : sol.solution) {
            for (var solData : inventoy) {
                solString.add(solData.getCommercial().getId());
            }

            solString.add(orienteeringData.getDepot().getId());
        }
        solString.removeLast();

        return solString.stream().mapToInt(Integer::intValue).toArray();
    }

    public SolverSolution getSolverSolution() {
        return solverSolution;
    }
}
