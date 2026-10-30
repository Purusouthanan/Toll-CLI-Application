package com.toll.model.admin;

/**
 * Represents an authorized highway administrative authority (e.g. NHAI Officer).
 */



public class Admin {
    private final String adminId;
    private final String name;
    private final String email;
    private final boolean isActive;

    public Admin(String adminId, String name, String email) {
        if (adminId == null || adminId.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin ID cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin name cannot be empty.");
        }
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.isActive = true;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "adminId='" + adminId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
