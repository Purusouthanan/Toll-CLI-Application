package com.toll.module2.account.model;

import com.toll.common.VehicleType;
import java.util.Objects;

/**
 * Abstract Base Class representing a Vehicle in the Toll Management System.
 * Demonstrates:
 * - Encapsulation: All fields are private, accessible strictly via getter/setter methods with validation.
 * - Inheritance: Extended by concrete vehicle types (Car, Van, Bus, Truck, MultiAxleVehicle).
 * - Polymorphism: Exposes abstract domain methods implemented uniquely by each child class.
 */
public abstract class Vehicle {
    private final String registrationNumber;
    private final VehicleType vehicleType;
    private String ownerName;
    private String phoneNumber;

    /**
     * Protected constructor for child classes.
     *
     * @param registrationNumber Unique vehicle registration number (e.g. MH-12-DE-1433, DL-01-AB-1234)
     * @param vehicleType        The standardized NHAI vehicle type
     * @param ownerName          Name of the registered vehicle owner
     * @param phoneNumber        Contact phone number of the vehicle owner
     */
    protected Vehicle(String registrationNumber, VehicleType vehicleType, String ownerName, String phoneNumber) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be null or empty.");
        }
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null.");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty.");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }

        this.registrationNumber = registrationNumber.trim().toUpperCase();
        this.vehicleType = vehicleType;
        this.ownerName = ownerName.trim();
        this.phoneNumber = phoneNumber.trim();
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be empty.");
        }
        this.ownerName = ownerName.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        this.phoneNumber = phoneNumber.trim();
    }

    /**
     * Polymorphic method: Number of axles for this vehicle type.
     */
    public abstract int getAxleCount();

    /**
     * Polymorphic method: Detailed description of the vehicle class and weight category.
     */
    public abstract String getCategoryDescription();

    /**
     * Polymorphic method: Returns true if this is an emergency vehicle (Ambulance, Fire Engine, etc.)
     */
    public boolean isEmergencyVehicle() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(registrationNumber, vehicle.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationNumber);
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - Owner: %s, Phone: %s",
                registrationNumber, vehicleType.getDisplayName(), ownerName, phoneNumber);
    }
}
