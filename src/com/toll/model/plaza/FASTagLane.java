package com.toll.model.plaza;

import com.toll.model.enums.LaneType;

/**
 * Concrete FASTag Lane equipped with RFID reader antenna and automatic boom barrier.
 * Demonstrates Inheritance from Lane.
 */
public class FASTagLane extends Lane {
    private final String rfidReaderId;

    public FASTagLane(int laneNumber) {
        super(laneNumber, LaneType.FASTAG);
        this.rfidReaderId = "RFID-READER-L" + laneNumber;
    }

    public FASTagLane(int laneNumber, String rfidReaderId) {
        super(laneNumber, LaneType.FASTAG);
        this.rfidReaderId = rfidReaderId;
    }

    public String getRfidReaderId() {
        return rfidReaderId;
    }

    @Override
    public String getLaneDescription() {
        return "FASTag Dedicated Lane #" + getLaneNumber() + " [RFID Scanner: " + rfidReaderId + ", Auto Barrier: Active]";
    }
}
