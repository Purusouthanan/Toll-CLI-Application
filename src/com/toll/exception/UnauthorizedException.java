package com.toll.exception;

/**
 * Thrown when an unauthorized user attempts an admin-only operation.
 */
public class UnauthorizedException extends TollManagementException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
