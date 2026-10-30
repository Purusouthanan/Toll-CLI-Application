package com.toll.module3.tollengine;

/**
 * Trip classifications for highway toll crossing.
 */
public enum TripType {
    SINGLE("Single Trip"),
    RETURN("Return Trip (Within 24 Hours)"),
    PASS("Monthly Pass Toll Exemption"),
    EMERGENCY("Emergency Vehicle Exemption");

    private final String description;

    TripType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
