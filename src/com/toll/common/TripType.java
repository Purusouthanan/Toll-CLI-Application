package com.toll.common;

public enum TripType {
    SINGLE("Single Trip (Standard Fare)"),
    RETURN("Return Trip (50% of Single Fare within 24h)"),
    PASS("Monthly Pass (Zero Fare at designated Plaza)"),
    EMERGENCY("Emergency Exemption (Zero Fare)");

    private final String description;

    TripType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
