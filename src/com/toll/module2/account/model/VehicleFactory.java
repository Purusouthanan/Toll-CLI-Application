package com.toll.module2.account.model;

import com.toll.common.VehicleType;

/**
 * Factory class for instantiating concrete Vehicle subclasses based on VehicleType.
 */
public final class VehicleFactory {

    private VehicleFactory() {
        // Utility / Factory class
    }

    public static Vehicle createVehicle(VehicleType type, String regNo, String ownerName, String phone) {
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null.");
        }
        return switch (type) {
            case CAR -> new Car(regNo, ownerName, phone);
            case VAN -> new Van(regNo, ownerName, phone);
            case BUS -> new Bus(regNo, ownerName, phone);
            case TRUCK -> new Truck(regNo, ownerName, phone);
            case MULTI_AXLE -> new MultiAxleVehicle(regNo, ownerName, phone, 3);
        };
    }

    public static Vehicle createMultiAxle(String regNo, String ownerName, String phone, int axles) {
        return new MultiAxleVehicle(regNo, ownerName, phone, axles);
    }
}
