package com.toll.module2.account.model;

import com.toll.common.VehicleType;

/**
 * Concrete representation of a Heavy Commercial Truck / Lorry (2-Axle).
 * Demonstrates Inheritance from Vehicle.
 */
public class Truck extends Vehicle {

    public Truck(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.TRUCK, ownerName, phoneNumber);
    }

    @Override
    public int getAxleCount() {
        return 2;
    }

    @Override
    public String getCategoryDescription() {
        return "Heavy Commercial 2-Axle Truck / Lorry";
    }
}
