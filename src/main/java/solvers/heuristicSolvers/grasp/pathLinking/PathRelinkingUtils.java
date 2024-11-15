package solvers.heuristicSolvers.grasp.pathLinking;

import data.Solution;

import runParameters.LocalSearchSettings;
import runParameters.PathRelinkingSettings;

import solvers.heuristicSolvers.grasp.localSearch.move.IMove;
import solvers.heuristicSolvers.grasp.localSearch.move.InsertMove;
import solvers.heuristicSolvers.grasp.localSearch.move.RemoveMove;
import solvers.heuristicSolvers.grasp.localSearch.move.TransferMove;

import java.util.ArrayList;
import java.util.Random;

@SuppressWarnings({"DuplicatedCode", "SpellCheckingInspection"})
public class PathRelinkingUtils {
    private final Random random;
    private final LocalSearchSettings localSearchSettings;
    private final PathRelinkingSettings pathRelinkingSettings;

    public PathRelinkingUtils(Random random, LocalSearchSettings localSearchSettings, PathRelinkingSettings pathRelinkingSettings) {
        this.random = random;
        this.localSearchSettings = localSearchSettings;
        this.pathRelinkingSettings = pathRelinkingSettings;
    }

    public int distance(Solution solution1, Solution solution2) {
        int numberOfCommercialsInDifferentInventories = 0;
        int numberOfCommercialsInCurrentButNotInGuiding = 0;
        int numberOfCommercialsInGuidingButNotInCurrent = 0;

        var solution1Datas = solution1.getSortedSolutionData();
        var solution2Datas = solution2.getSortedSolutionData();

        int i = 0, j = 0;
        while (i < solution1Datas.length && j < solution2Datas.length) {
            var solution1Data = solution1Datas[i];
            var solution2Data = solution2Datas[j];
            if (solution1Data == null) {
                i++;
                continue;
            }
            if (solution2Data == null) {
                j++;
                continue;
            }

            if (solution1Data.getCommercial().getId() == solution2Data.getCommercial().getId()) {
                if (solution1Data.getInventory().getId() != solution2Data.getInventory().getId()) {
                    numberOfCommercialsInDifferentInventories++;
                }
                i++;
                j++;
            } else if (solution1Data.getCommercial().getId()
                    < solution2Data.getCommercial().getId()) {
                numberOfCommercialsInCurrentButNotInGuiding++;
                i++;
            } else {
                numberOfCommercialsInGuidingButNotInCurrent++;
                j++;
            }
        }

        if (i < solution1Datas.length)
            numberOfCommercialsInCurrentButNotInGuiding += solution1Datas.length - i;
        if (j < solution2Datas.length)
            numberOfCommercialsInGuidingButNotInCurrent += solution2Datas.length - j;

        return numberOfCommercialsInDifferentInventories
                + numberOfCommercialsInCurrentButNotInGuiding
                + numberOfCommercialsInGuidingButNotInCurrent;
    }

    public IMove getMove(
            Solution currentSolution,
            Solution guidingSolution,
            double[] totalCommercialDurationOfHour) {
        IMove bestMove = null;
        var bestRevenueGain = Double.NEGATIVE_INFINITY;

        IMove randomMove = null;
        int count = 0;

        var solution1Datas = currentSolution.getSortedSolutionData();
        var solution2Datas = guidingSolution.getSortedSolutionData();

        int i = 0, j = 0;
        while (i < solution1Datas.length && j < solution2Datas.length) {
            var solution1Data = solution1Datas[i];
            var solution2Data = solution2Datas[j];

            if (solution1Data == null) {
                i++;
                continue;
            }
            if (solution2Data == null) {
                j++;
                continue;
            }

            // If commercial is in both solutions but in different inventories
            if (solution1Data.getCommercial().getId() == solution2Data.getCommercial().getId()) {
                if (solution1Data.getInventory().getId() != solution2Data.getInventory().getId()) {

                    var solutionDataListToTransfer =
                            currentSolution.solution.get(solution2Data.getInventory().getId());
                    var fromSolutionDataList =
                            currentSolution.solution.get(solution1Data.getInventory().getId());
                    var n1 = fromSolutionDataList.indexOf(solution1Data);

                    for (int k = 0; k < solutionDataListToTransfer.size(); k++) {
                        var move =
                                new TransferMove(
                                        currentSolution,
                                        solution1Data.getInventory(),
                                        solution2Data.getInventory(),
                                        n1,
                                        k,
                                        totalCommercialDurationOfHour);
                        if (move.checkFeasibility()) {
                            count++;
                            if (random.nextInt(count) == 0) randomMove = move;

                            if (move.calculateRevenueGain() > bestRevenueGain) {
                                bestRevenueGain = move.calculateRevenueGain();
                                bestMove = move;
                            }
                        }
                    }
                }
                i++;
                j++;
            }

            // If commercial is in current solution but not in guiding solution
            else if (solution1Data.getCommercial().getId()
                    < solution2Data.getCommercial().getId()) {
                var move =
                        new RemoveMove(
                                currentSolution,
                                solution1Data.getInventory(),
                                solution1Data.getPosition());
                if (move.checkFeasibility()) {
                    count++;
                    if (random.nextInt(count) == 0) randomMove = move;

                    if (move.calculateRevenueGain() > bestRevenueGain) {
                        bestRevenueGain = move.calculateRevenueGain();
                        bestMove = move;
                    }
                }
                i++;
            }

            // If commercial is in guiding solution but not in current solution
            else {
                for (var k = 0;
                        k
                                <= currentSolution
                                        .solution
                                        .get(solution2Data.getInventory().getId())
                                        .size();
                        k++) {
                    var move =
                            new InsertMove(
                                    currentSolution,
                                    solution2Data.getCommercial(),
                                    solution2Data.getInventory(),
                                    k,
                                    totalCommercialDurationOfHour);
                    if (move.checkFeasibility()) {
                        count++;
                        if (random.nextInt(count) == 0) randomMove = move;

                        if (move.calculateRevenueGain() > bestRevenueGain) {
                            bestRevenueGain = move.calculateRevenueGain();
                            bestMove = move;
                        }
                    }
                }
                j++;
            }
        }

        if (i < solution1Datas.length) {
            for (var k = i; k < solution1Datas.length; k++) {
                var solutionData = solution1Datas[k];
                if (solutionData == null) continue;

                var move =
                        new RemoveMove(
                                currentSolution,
                                solutionData.getInventory(),
                                solutionData.getPosition());
                if (move.checkFeasibility()) {
                    count++;
                    if (random.nextInt(count) == 0) randomMove = move;

                    if (move.calculateRevenueGain() > bestRevenueGain) {
                        bestRevenueGain = move.calculateRevenueGain();
                        bestMove = move;
                    }
                }
            }
        }

        if (j < solution2Datas.length) {
            for (var k = j; k < solution2Datas.length; k++) {
                var solutionData = solution2Datas[k];
                if (solutionData == null) continue;

                var solutionDataList =
                        currentSolution.solution.get(solutionData.getInventory().getId());
                for (var l = 0; l <= solutionDataList.size(); l++) {
                    var move =
                            new InsertMove(
                                    currentSolution,
                                    solutionData.getCommercial(),
                                    solutionData.getInventory(),
                                    l,
                                    totalCommercialDurationOfHour);
                    if (move.checkFeasibility()) {
                        count++;
                        if (random.nextInt(count) == 0) randomMove = move;

                        if (move.calculateRevenueGain() > bestRevenueGain) {
                            bestRevenueGain = move.calculateRevenueGain();
                            bestMove = move;
                        }
                    }
                }
            }
        }

        if (random.nextDouble() < pathRelinkingSettings.getCoeff() * localSearchSettings.randomMoveProbability) {
            return randomMove;
        }

        return bestMove;
    }
}
