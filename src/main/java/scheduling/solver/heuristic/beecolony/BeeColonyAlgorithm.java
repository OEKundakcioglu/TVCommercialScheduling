package scheduling.solver.heuristic.beecolony;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import scheduling.model.Problem;
import scheduling.solver.CheckPoint;
import scheduling.solver.SolverSolution;
import scheduling.solver.heuristic.HeuristicAlgorithm;
import scheduling.solver.heuristic.beecolony.move.NeighborhoodFunction;
import scheduling.solver.heuristic.beecolony.vrp.VrpConverter;
import scheduling.solver.heuristic.beecolony.vrp.VrpProblem;
import scheduling.solver.heuristic.grasp.construction.GreedyConstruction;

@RequiredArgsConstructor
public class BeeColonyAlgorithm implements HeuristicAlgorithm<BeeColonyInformation> {

    private final BeeColonyConfig config;
    private final Random random;

    @Override
    public SolverSolution<BeeColonyInformation> run(Problem problem) {
        var vrpProblem = VrpConverter.convert(problem);
        var evaluator = new FitnessEvaluator(vrpProblem);
        var neighborhoodFunction = new NeighborhoodFunction();

        var population = initializePopulation(problem, vrpProblem, evaluator);
        var totalFitness = calculateTotalFitness(population);
        var bestSolution = findBest(population);
        var checkPoints = new ArrayList<CheckPoint>();
        long startTime = System.currentTimeMillis();

        checkPoints.add(createCheckPoint(bestSolution.getFitness(), startTime));

        double temperature = config.initialTemperature();
        int generation = 0;

        while (elapsedSeconds(startTime) < config.timeLimitSeconds()) {
            generation++;

            totalFitness =
                    employedBeePhase(
                            population, neighborhoodFunction, evaluator, totalFitness, temperature);

            totalFitness =
                    onlookerBeePhase(
                            population, neighborhoodFunction, evaluator, totalFitness, temperature);

            var currentBest = findBest(population);
            if (currentBest.getFitness() > bestSolution.getFitness()) {
                bestSolution = currentBest;
                checkPoints.add(createCheckPoint(bestSolution.getFitness(), startTime));
            }

            if (generation % config.coolingInterval() == 0) {
                temperature = config.coolingCoefficient() * temperature;
            }
        }

        var solution = SolutionStringConverter.toSolution(bestSolution, vrpProblem);
        return new SolverSolution<>(solution, checkPoints, new BeeColonyInformation());
    }

    private List<BeeColonySolution> initializePopulation(
            Problem problem, VrpProblem vrpProblem, FitnessEvaluator evaluator) {
        var population = new ArrayList<BeeColonySolution>();
        for (int i = 0; i < config.populationSize(); i++) {
            var graspSolution = new GreedyConstruction(problem, 0.5, random, 0.5, 2.0).solve();
            var solString = SolutionStringConverter.toSolutionString(graspSolution, vrpProblem);
            var fitness = evaluator.evaluate(solString);
            population.add(new BeeColonySolution(solString, fitness));
        }
        return population;
    }

    private double employedBeePhase(
            List<BeeColonySolution> population,
            NeighborhoodFunction neighborhoodFunction,
            FitnessEvaluator evaluator,
            double totalFitness,
            double temperature) {
        for (int i = 0; i < population.size(); i++) {
            var sol = population.get(i);
            var newString = neighborhoodFunction.apply(sol.getSolutionString(), random);
            var newFitness = evaluator.evaluate(newString);
            var newSol = new BeeColonySolution(newString, newFitness);

            totalFitness = tryAccept(population, i, sol, newSol, totalFitness, temperature);
        }
        return totalFitness;
    }

    private double onlookerBeePhase(
            List<BeeColonySolution> population,
            NeighborhoodFunction neighborhoodFunction,
            FitnessEvaluator evaluator,
            double totalFitness,
            double temperature) {
        for (int i = 0; i < population.size(); i++) {
            var beeIndex = selectEmployedBee(population, totalFitness);
            var sol = population.get(beeIndex);
            var newString = neighborhoodFunction.apply(sol.getSolutionString(), random);
            var newFitness = evaluator.evaluate(newString);
            var newSol = new BeeColonySolution(newString, newFitness);

            totalFitness = tryAccept(population, beeIndex, sol, newSol, totalFitness, temperature);
        }
        return totalFitness;
    }

    private double tryAccept(
            List<BeeColonySolution> population,
            int index,
            BeeColonySolution oldSol,
            BeeColonySolution newSol,
            double totalFitness,
            double temperature) {
        double delta = (newSol.getFitness() - oldSol.getFitness()) / oldSol.getFitness();

        if (delta >= 0) {
            population.set(index, newSol);
            return totalFitness + newSol.getFitness() - oldSol.getFitness();
        }

        if (random.nextDouble() < Math.exp(delta / temperature)) {
            population.set(index, newSol);
            return totalFitness + newSol.getFitness() - oldSol.getFitness();
        }

        return totalFitness;
    }

    private int selectEmployedBee(List<BeeColonySolution> population, double totalFitness) {
        double r = random.nextDouble() * totalFitness;
        double currentSum = 0;
        for (int i = 0; i < population.size(); i++) {
            currentSum += population.get(i).getFitness();
            if (currentSum >= r) {
                return i;
            }
        }
        return population.size() - 1;
    }

    private BeeColonySolution findBest(List<BeeColonySolution> population) {
        var best = population.getFirst();
        for (var sol : population) {
            if (sol.getFitness() > best.getFitness()) {
                best = sol;
            }
        }
        return best;
    }

    private double calculateTotalFitness(List<BeeColonySolution> population) {
        double total = 0;
        for (var sol : population) {
            total += sol.getFitness();
        }
        return total;
    }

    private CheckPoint createCheckPoint(double objective, long startTimeMillis) {
        return new CheckPoint(objective, elapsedSeconds(startTimeMillis));
    }

    private double elapsedSeconds(long startTimeMillis) {
        return (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    }
}
