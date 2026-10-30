package com.toll.module2.account.model;

import com.toll.common.VehicleType;

/**
 * Concrete representation of a Bus / Coach.
 * Demonstrates Inheritance from Vehicle.
 */
public class Bus extends Vehicle {

    public Bus(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.BUS, ownerName, phoneNumber);
    }

    @Override
    public int getAxleCount() {
        return 2;
    }

    @Override
    public String getCategoryDescription() {
        return "Standard Heavy Commercial Bus (Gross Vehicle Weight 7,500 - 12,000 kg)";
    }
}
