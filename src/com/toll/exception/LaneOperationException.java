package com.toll.exception;

/**
 * Thrown when an illegal lane transition is attempted
 * (e.g. attempting to close a lane while a vehicle is actively being processed).
 */
public class LaneOperationException extends TollManagementException {
    public LaneOperationException(String message) {
        super(message);
    }
}
