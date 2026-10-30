package com.toll.module1.plaza.model;

import com.toll.common.LaneType;

/**
 * Concrete FASTag Electronic Toll Collection Lane.
 * Demonstrates Inheritance from Lane.
 */
public class FASTagLane extends Lane {
    public FASTagLane(int laneNumber) {
        super(laneNumber, LaneType.FASTAG);
    }

    @Override
    public String getLaneDescription() {
        return "Dedicated FASTag RFID Electronic Toll Collection (ETC) Lane";
    }
}
