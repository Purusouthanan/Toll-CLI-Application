package com.toll.model.enums;

/**
 * Operational status of a Toll Plaza.
 */
public enum PlazaStatus {
    INACTIVE("Plaza setup in progress; not open for public traffic"),
    ACTIVE("Plaza is operational with active lanes and rates");

    private final String description;

    PlazaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
