package com.toll.model.plaza;

import com.toll.exception.LaneOperationException;
import com.toll.model.enums.LaneStatus;
import com.toll.model.enums.LaneType;

/**
 * Abstract base class representing a toll lane.
 * Demonstrates Object-Oriented Inheritance and Encapsulation.
 */
public abstract class Lane {
    private final int laneNumber;
    private final LaneType laneType;
    private LaneStatus status;
    private boolean isProcessingVehicle;

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
        this.isProcessingVehicle = false;
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
        return isProcessingVehicle;
    }

    /**
     * Opens the lane for traffic.
     */
    public synchronized void openLane() {
        this.status = LaneStatus.OPEN;
    }

    /**
     * Closes the lane for traffic.
     * Business Rule: A lane can be closed only if no vehicle is being processed in it.
     */
    public synchronized void closeLane() {
        if (this.isProcessingVehicle) {
            throw new LaneOperationException(
                    "Violation: Lane " + laneNumber + " (" + laneType + ") cannot be closed while a vehicle is being processed."
            );
        }
        this.status = LaneStatus.CLOSED;
    }

    /**
     * Marks that a vehicle has entered the lane and is undergoing toll processing.
     */
    public synchronized void startProcessingVehicle() {
        if (this.status == LaneStatus.CLOSED) {
            throw new LaneOperationException("Cannot process vehicle: Lane " + laneNumber + " is currently CLOSED.");
        }
        if (this.isProcessingVehicle) {
            throw new LaneOperationException("Lane " + laneNumber + " is already processing a vehicle.");
        }
        this.isProcessingVehicle = true;
    }

    /**
     * Marks that the vehicle has completed toll processing and cleared the lane barrier.
     */
    public synchronized void finishProcessingVehicle() {
        this.isProcessingVehicle = false;
    }

    /**
     * Polymorphic method to return specialized lane operational details.
     */
    public abstract String getLaneDescription();

    @Override
    public String toString() {
        return String.format("Lane[#%d | %s | Status: %s | ActiveVehicle: %s]",
                laneNumber, laneType, status, isProcessingVehicle);
    }
}
