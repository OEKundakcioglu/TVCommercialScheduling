package data;

import runParameters.LoopSetup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Solution {
    public transient List<List<SolutionData>> solution;
    private transient SolutionData[] flattenedSortedSolutionData;
    public double revenue;

    public Solution(List<List<SolutionData>> solution) {
        this.solution = solution;

        for (List<SolutionData> solutionDataList : solution) {
            if (solutionDataList == null) continue;
            calculateKpiValues(solutionDataList);
        }

        this.revenue = solution.stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .mapToDouble(SolutionData::getRevenue).sum();


        this.flattenedSortedSolutionData = new SolutionData[LoopSetup.numberOfCommercials];
        for (List<SolutionData> solutionDataList : solution) {
            if (solutionDataList == null) continue;
            for (SolutionData solutionData : solutionDataList) {
                flattenedSortedSolutionData[solutionData.getCommercial().getId()] = solutionData;
            }
        }
    }

    private Solution(List<List<SolutionData>> solution,
                     SolutionData[] flattenedSortedSolutionData,
                     double revenue) {
        this.solution = solution;
        this.flattenedSortedSolutionData = flattenedSortedSolutionData;
        this.revenue = revenue;
    }

    private void calculateKpiValues(List<SolutionData> solutionDataList){
        var currentTime = 0;

        for (var i = 0; i < solutionDataList.size(); i++){
            var solutionData = solutionDataList.get(i);
            var inventory = solutionData.getInventory();
            var commercial = solutionData.getCommercial();

            var minute = currentTime / 60 + 1;

            double rating;
            try{
                rating = inventory.arrayRatings[minute][commercial.getAudienceType()];
            }
            catch (NullPointerException e){
                rating = 0;
            }
            solutionData.update(commercial.getRevenue(rating), currentTime, i);

            currentTime += commercial.getDuration();
        }
    }

    public Solution copy() {
        var newSolution = new ArrayList<List<SolutionData>>();
        var newFlattenedSolutionDataList = new SolutionData[LoopSetup.numberOfCommercials];

        for (var oldSolutionDataList : solution) {
            var solutionDataList = new ArrayList<SolutionData>();
            for (SolutionData solutionData : oldSolutionDataList) {
                var copy = solutionData.copy();
                solutionDataList.add(copy);
                newFlattenedSolutionDataList[copy.getCommercial().getId()] = copy;
            }
            newSolution.add(solutionDataList);
        }


        return new Solution(newSolution,
                newFlattenedSolutionDataList,
                this.revenue);
    }

    public void addSolutionData(Inventory inventory, SolutionData solutionData, int toIndex) {
        if (solutionData.getCommercial().getId() >= flattenedSortedSolutionData.length) {
            flattenedSortedSolutionData = Arrays.copyOf(flattenedSortedSolutionData, solutionData.getCommercial().getId()+1);
        }

        solution.get(inventory.getId()).add(toIndex, solutionData);
        flattenedSortedSolutionData[solutionData.getCommercial().getId()] = solutionData;
    }

    public void removeSolutionData(Inventory inventory, SolutionData solutionData) {
        var isRemoved = solution.get(inventory.getId()).remove(solutionData);
        if (!isRemoved) {
            throw new IllegalArgumentException("Solution data is not in the solution");
        }

        flattenedSortedSolutionData[solutionData.getCommercial().getId()] = null;
    }

    public SolutionData[] getSortedSolutionData() {
        return flattenedSortedSolutionData;
    }


    public SolutionData getSolutionData(Commercial commercial){
        return flattenedSortedSolutionData[commercial.getId()];
    }

}
