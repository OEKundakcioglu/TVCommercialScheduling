package solvers.heuristicSolvers.grasp.pathLinking;

import data.ProblemParameters;
import data.Solution;
import data.Utils;

public class MixedPathRelinking {

    private final Solution guidingSolution;
    private final Solution initialSolution;
    private final ProblemParameters parameters;
    private final PathRelinkingUtils pathRelinkingUtils;

    private Solution bestFoundSolution;
    private double bestFoundRevenue = Double.NEGATIVE_INFINITY;

    public MixedPathRelinking(
            ProblemParameters parameters,
            Solution initialSolution,
            Solution guidingSolution,
            PathRelinkingUtils pathRelinkingUtils)
            throws Exception {
        this.parameters = parameters;
        this.initialSolution = initialSolution;
        this.guidingSolution = guidingSolution;
        this.pathRelinkingUtils = pathRelinkingUtils;
        this.bestFoundSolution = initialSolution;

        solve();
    }

    public void solve() throws Exception {
        var currentSolution = initialSolution;
        var _guidingSolution = guidingSolution;

        var anyFeasibleMoveFromGuiding = false;
        var anyFeasibleMoveFromCurrent = false;

        var isDirectionTowardsGuiding = true;

        while (pathRelinkingUtils.distance(currentSolution, _guidingSolution) > 1) {
            var totalCommercialDurationOfHour =
                    Utils.getHourlyDurations(currentSolution, parameters);
            var target = isDirectionTowardsGuiding ? guidingSolution : initialSolution;
            var move =
                    pathRelinkingUtils.getMove(
                            currentSolution, target, totalCommercialDurationOfHour);
            if (move == null) {
                var temp = currentSolution;
                currentSolution = _guidingSolution;
                _guidingSolution = temp;
                if (isDirectionTowardsGuiding) anyFeasibleMoveFromCurrent = false;
                else anyFeasibleMoveFromGuiding = false;
                isDirectionTowardsGuiding = !isDirectionTowardsGuiding;

                if (!anyFeasibleMoveFromCurrent && !anyFeasibleMoveFromGuiding) break;

                continue;
            }

            currentSolution = move.applyMove();
            if (currentSolution.revenue > bestFoundRevenue) {
                bestFoundSolution = currentSolution;
                bestFoundRevenue = currentSolution.revenue;
            }

            var temp = currentSolution;
            currentSolution = _guidingSolution;
            pathRelinkingUtils.distance(currentSolution, _guidingSolution);
            _guidingSolution = temp;
            if (isDirectionTowardsGuiding) anyFeasibleMoveFromCurrent = true;
            else anyFeasibleMoveFromGuiding = true;
            isDirectionTowardsGuiding = !isDirectionTowardsGuiding;
        }
    }

    public Solution getBestFoundSolution() {
        return bestFoundSolution;
    }
}
