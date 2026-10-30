package com.toll.model.enums;

/**
 * Standard vehicle classifications recognized across Indian National Highways (NHAI / MoRTH).
 */
public enum VehicleType {
    CAR("Car / Jeep / Van"),
    VAN("Light Commercial Vehicle / Mini-Bus"),
    BUS("Bus (2-Axle)"),
    TRUCK("Truck (2-Axle)"),
    MULTI_AXLE("Multi-Axle Commercial Vehicle (3 to 6 Axles)");

    private final String description;

    VehicleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
