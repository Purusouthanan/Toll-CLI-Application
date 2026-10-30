package com.toll.module2.account.model;

import com.toll.common.VehicleType;

/**
 * Concrete representation of a Car / Jeep / Personal SUV.
 * Demonstrates Inheritance from Vehicle.
 */
public class Car extends Vehicle {

    public Car(String registrationNumber, String ownerName, String phoneNumber) {
        super(registrationNumber, VehicleType.CAR, ownerName, phoneNumber);
    }

    @Override
    public int getAxleCount() {
        return 2;
    }

    @Override
    public String getCategoryDescription() {
        return "Passenger Car / Jeep / SUV (Gross Vehicle Weight < 3,500 kg)";
    }
}
