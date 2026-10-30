package com.toll.module2.account.model;

import com.toll.common.VehicleType;

/**
 * Concrete representation of a Multi-Axle Vehicle (MAV) / Heavy Construction Machinery (HCM).
 * Demonstrates Inheritance from Vehicle.
 */
public class MultiAxleVehicle extends Vehicle {
    private final int axleCount;

    public MultiAxleVehicle(String registrationNumber, String ownerName, String phoneNumber) {
        this(registrationNumber, ownerName, phoneNumber, 3);
    }

    public MultiAxleVehicle(String registrationNumber, String ownerName, String phoneNumber, int axleCount) {
        super(registrationNumber, VehicleType.MULTI_AXLE, ownerName, phoneNumber);
        if (axleCount < 3) {
            throw new IllegalArgumentException("Multi-Axle vehicles must have at least 3 axles.");
        }
        this.axleCount = axleCount;
    }

    @Override
    public int getAxleCount() {
        return axleCount;
    }

    @Override
    public String getCategoryDescription() {
        return String.format("Multi-Axle Vehicle (MAV) with %d axles / Heavy Machinery", axleCount);
    }
}
