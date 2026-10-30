package com.toll.model.enums;

/**
 * Trip type classification for toll fee computation.
 */
public enum TripType {
    SINGLE("Single One-Way Journey"),
    RETURN("Return Journey within 24 Hours");

    private final String description;

    TripType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
