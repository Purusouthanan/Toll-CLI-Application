package com.toll.module2.account.exception;

public class DuplicateVehicleException extends FASTagAccountException {
    public DuplicateVehicleException(String registrationNumber) {
        super("Vehicle with registration number '" + registrationNumber + "' is already registered in the system.");
    }
}
