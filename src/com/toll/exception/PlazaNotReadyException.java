package com.toll.exception;

/**
 * Thrown when a toll plaza cannot be opened due to violation of business prerequisites
 * (e.g. less than 2 lanes, or missing toll rates for any vehicle type).
 */
public class PlazaNotReadyException extends TollManagementException {
    public PlazaNotReadyException(String message) {
        super(message);
    }
}
