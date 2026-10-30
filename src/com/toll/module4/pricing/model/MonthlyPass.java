package com.toll.module4.pricing.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing an NHAI Monthly Toll Pass.
 * Rule: Valid pass yields Rs. 0 toll at the specified plaza.
 * Rule: Only one valid pass per vehicle per plaza at a time.
 */
public class MonthlyPass {
    private final String passId;
    private final String vehicleRegistrationNumber;
    private final int plazaId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double feePaid;

    public MonthlyPass(String passId, String vehicleRegistrationNumber, int plazaId,
                       LocalDate startDate, LocalDate endDate, double feePaid) {
        if (passId == null || passId.trim().isEmpty()) throw new IllegalArgumentException("Pass ID cannot be empty.");
        if (vehicleRegistrationNumber == null || vehicleRegistrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be empty.");
        }
        if (startDate == null || endDate == null) throw new IllegalArgumentException("Start and End dates cannot be null.");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("End date cannot be before start date.");
        if (feePaid < 0.0) throw new IllegalArgumentException("Pass fee cannot be negative.");

        this.passId = passId.trim().toUpperCase();
        this.vehicleRegistrationNumber = vehicleRegistrationNumber.trim().toUpperCase();
        this.plazaId = plazaId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.feePaid = feePaid;
    }

    public String getPassId() {
        return passId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getFeePaid() {
        return feePaid;
    }

    public boolean isValidOn(LocalDate queryDate) {
        if (queryDate == null) return false;
        return !queryDate.isBefore(startDate) && !queryDate.isAfter(endDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonthlyPass that = (MonthlyPass) o;
        return Objects.equals(passId, that.passId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passId);
    }

    @Override
    public String toString() {
        return String.format("MonthlyPass[%s | Reg: %s | Plaza: #%d | Valid: %s to %s | Fee: Rs. %.2f]",
                passId, vehicleRegistrationNumber, plazaId, startDate, endDate, feePaid);
    }
}
