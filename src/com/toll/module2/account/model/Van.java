package com.toll.module2.account.model;

import com.toll.common.VehicleType;

/**
 * Concrete representation of a Van / Light Commercial Vehicle.
 * Demonstrates Inheritance from Vehicle.
 */
public class Van extends Vehicle {

    public Van(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.VAN, ownerName, phoneNumber);
    }

    @Override
    public int getAxleCount() {
        return 2;
    }

    @Override
    public String getCategoryDescription() {
        return "Light Commercial Vehicle (LCV) / Passenger Van";
    }
}
