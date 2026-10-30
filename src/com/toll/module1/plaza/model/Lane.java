package com.toll.module1.plaza.model;

import com.toll.common.LaneStatus;
import com.toll.common.LaneType;

/**
 * Abstract Base Class representing a Toll Plaza Lane.
 * Demonstrates:
 * - Encapsulation: Private state mutated only through validated methods.
 * - Inheritance: Extended by FASTagLane, CashLane, and EmergencyLane.
 * - Safety Rule: A lane can be closed ONLY if no vehicle is currently being processed.
 */
public abstract class Lane {
    private final int laneNumber;
    private final LaneType laneType;
    private LaneStatus status;
    private boolean processingVehicle;

    protected Lane(int laneNumber, LaneType laneType) {
        if (laneNumber <= 0) {
            throw new IllegalArgumentException("Lane number must be positive.");
        }
        if (laneType == null) {
            throw new IllegalArgumentException("Lane type cannot be null.");
        }
        this.laneNumber = laneNumber;
        this.laneType = laneType;
        this.status = LaneStatus.OPEN;
        this.processingVehicle = false;
    }

    public int getLaneNumber() {
        return laneNumber;
    }

    public LaneType getLaneType() {
        return laneType;
    }

    public synchronized LaneStatus getStatus() {
        return status;
    }

    public synchronized boolean isProcessingVehicle() {
        return processingVehicle;
    }

    public synchronized void setProcessingVehicle(boolean processingVehicle) {
        this.processingVehicle = processingVehicle;
    }

    public synchronized void open() {
        this.status = LaneStatus.OPEN;
    }

    /**
     * Safety close rule: A lane can be closed ONLY if no vehicle is being processed in it.
     */
    public synchronized boolean close() {
        if (this.processingVehicle) {
            return false; // Rejected because vehicle is in progress
        }
        this.status = LaneStatus.CLOSED;
        return true;
    }

    public abstract String getLaneDescription();

    @Override
    public String toString() {
        return String.format("Lane #%d [%s | Status: %s | Busy: %s]",
                laneNumber, laneType, status, processingVehicle ? "YES" : "NO");
    }
}
