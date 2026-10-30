package com.toll.model.enums;

/**
 * Operational status of a lane.
 */
public enum LaneStatus {
    OPEN("Lane is open for vehicle traffic"),
    CLOSED("Lane is closed and blocked by barrier/signals");

    private final String description;

    LaneStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
