package com.toll.exception;

/**
 * Base unchecked exception for Toll Management System.
 */
public class TollManagementException extends RuntimeException {
    public TollManagementException(String message) {
        super(message);
    }

    public TollManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
