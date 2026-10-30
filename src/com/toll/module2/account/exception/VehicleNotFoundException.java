package com.toll.module2.account.exception;

public class VehicleNotFoundException extends FASTagAccountException {
    public VehicleNotFoundException(String registrationNumber) {
        super("Vehicle with registration number '" + registrationNumber + "' was not found.");
    }
}
