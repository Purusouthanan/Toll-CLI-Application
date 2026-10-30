package com.toll.module1.plaza.model;

import com.toll.common.LaneType;

/**
 * Concrete Emergency Lane for zero-toll transit of Ambulances, Fire Engines, etc.
 * Demonstrates Inheritance from Lane.
 */
public class EmergencyLane extends Lane {
    public EmergencyLane(int laneNumber) {
        super(laneNumber, LaneType.EMERGENCY);
    }

    @Override
    public String getLaneDescription() {
        return "Dedicated Priority Zero-Fare Lane for Emergency Vehicles";
    }
}
