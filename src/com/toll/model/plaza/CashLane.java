package com.toll.model.plaza;

import com.toll.model.enums.LaneType;

/**
 * Concrete Cash Lane with POS terminal for manual cash toll collection.
 * Demonstrates Inheritance from Lane.
 */
public class CashLane extends Lane {
    private String currentOperatorName;

    public CashLane(int laneNumber) {
        super(laneNumber, LaneType.CASH);
        this.currentOperatorName = "Default Booth Operator";
    }

    public CashLane(int laneNumber, String currentOperatorName) {
        super(laneNumber, LaneType.CASH);
        this.currentOperatorName = currentOperatorName;
    }

    public synchronized String getCurrentOperatorName() {
        return currentOperatorName;
    }

    public synchronized void assignOperator(String operatorName) {
        if (operatorName == null || operatorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Operator name cannot be empty.");
        }
        this.currentOperatorName = operatorName;
    }

    @Override
    public String getLaneDescription() {
        return "Manual Cash Lane #" + getLaneNumber() + " [Operator: " + currentOperatorName + ", POS: Online]";
    }
}
