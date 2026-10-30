package com.toll.module1.plaza.model;

import com.toll.common.LaneType;

/**
 * Concrete Cash Collection Lane for manual payments or fallback.
 * Demonstrates Inheritance from Lane.
 */
public class CashLane extends Lane {
    public CashLane(int laneNumber) {
        super(laneNumber, LaneType.CASH);
    }

    @Override
    public String getLaneDescription() {
        return "Manual Cash Payment Lane operated by Toll Booth Operator";
    }
}
