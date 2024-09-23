package localSearch.move;

import data.Solution;

public interface IMove {
    Solution applyMove() throws Exception;
    double calculateRevenueGain();
    boolean checkFeasibility();
    Solution getSolution();
}
