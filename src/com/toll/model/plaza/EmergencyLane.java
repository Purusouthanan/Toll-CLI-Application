package com.toll.model.plaza;

import com.toll.model.enums.LaneType;

/**
 * Concrete Emergency Lane reserved for emergency response vehicles (Ambulance, Fire Engine, Police).
 * Demonstrates Inheritance from Lane.
 */
public class EmergencyLane extends Lane {
    public EmergencyLane(int laneNumber) {
        super(laneNumber, LaneType.EMERGENCY);
    }

    @Override
    public String getLaneDescription() {
        return "Priority Emergency Lane #" + getLaneNumber() + " [Reserved for Emergency Vehicles - 100% Toll Exempt]";
    }
}
