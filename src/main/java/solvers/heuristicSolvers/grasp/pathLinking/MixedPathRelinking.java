package solvers.heuristicSolvers.grasp.pathLinking;

import data.Solution;
import data.Utils;
import solvers.heuristicSolvers.grasp.localSearch.move.IMove;
import data.ProblemParameters;

import java.util.ArrayList;
import java.util.List;

public class MixedPathRelinking {

    private final Solution guidingSolution;
    private final Solution initialSolution;
    private Solution bestFoundSolution;
    private final ProblemParameters parameters;
    public List<IMove> madeMoves = new ArrayList<>();

    public MixedPathRelinking(ProblemParameters parameters, Solution initialSolution, Solution guidingSolution) throws Exception {
        this.parameters = parameters;
        this.initialSolution = initialSolution;
        this.guidingSolution = guidingSolution;

        solve();
    }

    public void solve() throws Exception {
        var currentSolution = initialSolution;
        var _guidingSolution = guidingSolution;
        this.bestFoundSolution = currentSolution;

        var anyFeasibleMoveFromGuiding = false;
        var anyFeasibleMoveFromCurrent = false;

        var isDirectionTowardsGuiding = true;

        while (PathRelinkingUtils.distance(currentSolution, _guidingSolution) > 1){
            var totalCommercialDurationOfHour = Utils.getHourlyDurations(currentSolution, parameters);
            var target = isDirectionTowardsGuiding ? guidingSolution : initialSolution;
            var move = PathRelinkingUtils.getMove(currentSolution, target, totalCommercialDurationOfHour);
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
            madeMoves.add(move);
            if (currentSolution.revenue > bestFoundSolution.revenue) {
                bestFoundSolution = currentSolution;
            }

            var temp = currentSolution;
            currentSolution = _guidingSolution;
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
