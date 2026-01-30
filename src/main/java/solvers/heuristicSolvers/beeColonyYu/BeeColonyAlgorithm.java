package solvers.heuristicSolvers.beeColonyYu;

import data.ProblemParameters;
import runParameters.ConstructiveHeuristicSettings;
import solvers.CheckPoint;
import solvers.SolverSolution;
import solvers.heuristicSolvers.beeColonyYu.localSearch.NeighborhoodFunction;
import solvers.heuristicSolvers.grasp.constructiveHeuristic.ConstructiveHeuristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BeeColonyAlgorithm {
    private final OrienteeringData orienteeringData;
    private final BeeColonyUtils beeColonyUtils;
    private final ProblemParameters parameters;
    private final List<BeeColonySolution> employedBees;
    private final BeeColonySettings beeColonySettings;
    private final List<BeeColonyCheckPoint> checkPoints;
    private final SolverSolution solverSolution;
    private BeeColonySolution bestSolution;
    private double totalFitness = 0;

    private final Random random;

    public BeeColonyAlgorithm(
            OrienteeringData orienteeringData,
            BeeColonySettings beeColonySettings,
            ProblemParameters parameters,
            Random random) {
        this.orienteeringData = orienteeringData;
        this.beeColonySettings = beeColonySettings;
        this.parameters = parameters;
        this.checkPoints = new ArrayList<>();
        this.random = random;

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
        for (var i = 0; i < beeColonySettings.populationSize(); i++) {
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
        var neighborhoodFunction = new NeighborhoodFunction(beeColonyUtils, random);

        int G = 0;
        double T = beeColonySettings.T0();

        long startTime = System.currentTimeMillis() / 1000;

        while (System.currentTimeMillis() / 1000 - startTime < beeColonySettings.timeLimit()) {
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
                                newSol, (System.currentTimeMillis() / 1000 - startTime), G);
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
                                newSol, (System.currentTimeMillis() / 1000 - startTime), G);
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
        }
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
            BeeColonySolution solution, long passedTime, int nIter) {
        this.bestSolution = solution;
        this.checkPoints.add(new BeeColonyCheckPoint(solution, passedTime));
    }

    private int[] generateRandomSolution() {
        var constructive =
                new ConstructiveHeuristic(
                        parameters, 0.5, new ConstructiveHeuristicSettings(2), random);
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
