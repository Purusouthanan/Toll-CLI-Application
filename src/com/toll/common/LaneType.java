package com.toll.common;

public enum LaneType {
    FASTAG("FASTag Electronic Toll Collection"),
    CASH("Cash / Manual Collection"),
    EMERGENCY("Emergency Vehicle Dedicated Lane");

    private final String description;

    LaneType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
