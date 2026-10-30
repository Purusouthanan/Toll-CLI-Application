package com.toll.model.enums;

/**
 * Types of lanes available at a toll plaza.
 */
public enum LaneType {
    FASTAG("Electronic Toll Collection (FASTag RFID) Lane"),
    CASH("Manual Cash & Penalty Collection Lane"),
    EMERGENCY("Dedicated Free Emergency Vehicle Lane (Ambulance, Fire Engine)");

    private final String description;

    LaneType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
