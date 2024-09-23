package grasp.graspWithPathRelinking;

import constructiveHeuristic.ConstructiveHeuristic;
import data.Solution;
import grasp.CheckPoint;
import grasp.GraspOutput;
import grasp.IGrasp;
import grasp.pathLinking.MixedPathRelinking;
import grasp.pathLinking.PathRelinkingUtils;
import localSearch.LocalSearch;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import model.ProblemParameters;
import runParameters.GraspSettings;

import java.util.*;

public class ReactiveGraspWithPathRelinking implements IGrasp {
    @SuppressWarnings("FieldCanBeLocal")
    private final int eliteSolutionsSize = 5;

    private final ProblemParameters parameters;
    private Solution bestSolution;
    private final List<Solution> eliteSolutions;
    private int foundSolutionAt;
    private final Random random;
    private final GraspSettings graspSettings;
    private final GraspOutput graspOutput;

    public ReactiveGraspWithPathRelinking(ProblemParameters parameters, GraspSettings graspSettings) throws Exception {
        random = graspSettings.random();

        this.parameters = parameters;
        this.eliteSolutions = new LinkedList<>();
        this.graspSettings = graspSettings;

        this.graspOutput = new GraspOutput(
                parameters.getSetOfCommercials(),
                parameters.getSetOfInventories(),
                new ArrayList<>(),
                graspSettings,
                null
        );

        this.solve();
    }

    private void solve() throws Exception {
        double iterationsPerSecond = 0;
        this.bestSolution =
                new ConstructiveHeuristic(parameters, this.graspSettings.alphaGenerator().generateAlpha(),
        random, graspSettings.constructiveHeuristicSettings())
                        .getSolution();

        ProgressBar pb = new ProgressBarBuilder()
                .setUnit("s", 1)
                .setInitialMax(graspSettings.timeLimit())
                .hideEta()
                .setConsumer(new ConsoleProgressBarConsumer(System.out, 120))
                .setTaskName("Reactive Grasp with Path Relinking")
                .build();

        var startTime = System.currentTimeMillis() / 1000;

        int iteration = 1;
        while(System.currentTimeMillis() / 1000 - startTime < graspSettings.timeLimit()) {
            var randomSolution = new ConstructiveHeuristic(parameters, this.graspSettings.alphaGenerator().generateAlpha(), random, graspSettings.constructiveHeuristicSettings()).getSolution();
            randomSolution = new LocalSearch(randomSolution, parameters, graspSettings.isBestMove(), graspSettings.localSearchSettings(), random).getSolution();

            if (this.eliteSolutions.size() > 2) {
                var initialSolution = randomSolution;
                var guidingSolution = getGuidingSolution();

                randomSolution = new MixedPathRelinking(parameters, initialSolution, guidingSolution).getBestFoundSolution();

                randomSolution = new LocalSearch(randomSolution, parameters, graspSettings.isBestMove(), graspSettings.localSearchSettings(), random).getSolution();
            }

            this.updateEliteSolutions(randomSolution, pb, startTime, iterationsPerSecond);

            pb.stepTo((int) (System.currentTimeMillis() / 1000 - startTime));

            if (iteration % 10 == 0){
                var _iterationsPerSecond = iteration / (double) (System.currentTimeMillis() / 1000 - startTime);
                if (iterationsPerSecond != _iterationsPerSecond)
                    pb.setExtraMessage(String.format("%.2f iter/s Best solution: %d found at %d seconds", _iterationsPerSecond, (int) bestSolution.revenue, foundSolutionAt));
                iterationsPerSecond = _iterationsPerSecond;
            }

            iteration ++;
        }
        pb.stepTo(graspSettings.timeLimit());
        pb.close();

        this.graspOutput.setAvgIterationPerSecond((double) iteration / graspSettings.timeLimit());
    }

    private void updateEliteSolutions(Solution newFoundLocalOptima, ProgressBar pb, long currentTime, double iterationsPerSecond) {
        if (newFoundLocalOptima.revenue > bestSolution.revenue) {
            this.foundSolutionAt = (int) (System.currentTimeMillis() / 1000 - currentTime);
            bestSolution = newFoundLocalOptima;
            pb.setExtraMessage(String.format("%.2fiter/s Best solution: %d found at %d seconds", iterationsPerSecond, (int) bestSolution.revenue, (System.currentTimeMillis() / 1000 - currentTime)));

            this.graspOutput.getSolutionMemory().add(new CheckPoint(newFoundLocalOptima, ((double) System.currentTimeMillis() / 1000 - currentTime)));
            this.graspOutput.setBestSolution(newFoundLocalOptima);
        }

        if (eliteSolutions.size() < eliteSolutionsSize) {
            if (eliteSolutions.isEmpty())
                eliteSolutions.add(newFoundLocalOptima);
            else{
                var minDistance = eliteSolutions.stream()
                        .map(solution -> PathRelinkingUtils.distance(solution, newFoundLocalOptima))
                        .min(Comparator.comparing(distance -> distance))
                        .orElseThrow();
                if (minDistance > 0) eliteSolutions.add(newFoundLocalOptima);
            }
        } else {
            int minDistance = eliteSolutions.stream()
                    .map(solution -> PathRelinkingUtils.distance(solution, newFoundLocalOptima))
                    .min(Comparator.comparing(distance -> distance))
                    .orElseThrow();

            if (minDistance == 0) return;

            var worstSolution = eliteSolutions.stream()
                    .min(Comparator.comparing(solution -> solution.revenue))
                    .orElseThrow();
            if (minDistance > 0 && newFoundLocalOptima.revenue > worstSolution.revenue) {
                eliteSolutions.remove(worstSolution);
                eliteSolutions.add(newFoundLocalOptima);
            }
        }
    }

    public Solution getSolution() {
        return bestSolution;
    }

    private Solution getGuidingSolution(){
        return eliteSolutions.get(random.nextInt(eliteSolutions.size()));
    }

    public GraspOutput getGraspOutput() {
        return graspOutput;
    }
}
