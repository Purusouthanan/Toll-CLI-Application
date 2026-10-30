package com.toll.common;

public enum PlazaStatus {
    INACTIVE("Inactive - Configuration pending"),
    ACTIVE("Active - Open for toll processing");

    private final String description;

    PlazaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
