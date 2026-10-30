package com.toll.common;

/**
 * Lifecycle status of a FASTag account.
 * ACTIVE: Account is operational and eligible for automated toll deduction.
 * BLOCKED: Account balance dropped below Rs. 100, or tag is suspended.
 * CLOSED: Account is permanently deactivated.
 */
public enum AccountStatus {
    ACTIVE("Active - Ready for toll processing"),
    BLOCKED("Blocked - Balance < Rs. 100 or manually suspended"),
    CLOSED("Closed - Deactivated");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
