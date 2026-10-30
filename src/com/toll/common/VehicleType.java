package com.toll.common;

/**
 * Enumeration representing standard Indian highway vehicle classifications
 * as recognized by NHAI and the Toll Management System.
 */
public enum VehicleType {
    CAR("Car / Jeep / Van", 2, "Light Motor Vehicle (LMV)"),
    VAN("Van / Mini-Bus", 2, "Light Commercial Vehicle (LCV)"),
    BUS("Standard Bus", 2, "Heavy Commercial Vehicle - 2 Axle"),
    TRUCK("Truck / Lorry", 2, "Heavy Commercial Vehicle - 2 Axle"),
    MULTI_AXLE("Multi-Axle Vehicle (MAV)", 3, "Heavy Construction Machinery / Multi-Axle");

    private final String displayName;
    private final int defaultAxles;
    private final String category;

    VehicleType(String displayName, int defaultAxles, String category) {
        this.displayName = displayName;
        this.defaultAxles = defaultAxles;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultAxles() {
        return defaultAxles;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
