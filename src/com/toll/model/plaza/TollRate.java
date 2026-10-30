package com.toll.model.plaza;

import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a toll rate configuration for a specific plaza, vehicle type, and trip type,
 * governed by an effective date.
 * Demonstrates Encapsulation and Immutable Value Modeling.
 */
public class TollRate {
    private final String rateId;
    private final int plazaId;
    private final VehicleType vehicleType;
    private final TripType tripType;
    private final double amount;
    private final LocalDate effectiveDate;

    public TollRate(int plazaId, VehicleType vehicleType, TripType tripType, double amount, LocalDate effectiveDate) {
        if (plazaId <= 0) {
            throw new IllegalArgumentException("Plaza ID must be positive.");
        }
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null.");
        }
        if (tripType == null) {
            throw new IllegalArgumentException("Trip type cannot be null.");
        }
        if (amount < 0.0) {
            throw new IllegalArgumentException("Toll rate amount cannot be negative.");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("Effective date cannot be null.");
        }

        this.rateId = String.format("RATE-%d-%s-%s-%s", plazaId, vehicleType.name(), tripType.name(), effectiveDate);
        this.plazaId = plazaId;
        this.vehicleType = vehicleType;
        this.tripType = tripType;
        this.amount = amount;
        this.effectiveDate = effectiveDate;
    }

    public String getRateId() {
        return rateId;
    }

    public int getPlazaId() {
        return plazaId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public TripType getTripType() {
        return tripType;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    /**
     * Determines if this rate is valid and applicable for a crossing date.
     * Business Rule: A rate change applies only on or after its effective date.
     */
    public boolean isApplicableOn(LocalDate crossingDate) {
        if (crossingDate == null) {
            return false;
        }
        return !crossingDate.isBefore(this.effectiveDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TollRate tollRate = (TollRate) o;
        return plazaId == tollRate.plazaId &&
                Double.compare(tollRate.amount, amount) == 0 &&
                vehicleType == tollRate.vehicleType &&
                tripType == tollRate.tripType &&
                Objects.equals(effectiveDate, tollRate.effectiveDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plazaId, vehicleType, tripType, amount, effectiveDate);
    }

    @Override
    public String toString() {
        return String.format("TollRate[Plaza: %d | Vehicle: %-10s | Trip: %-6s | Fee: ₹%.2f | Effective: %s]",
                plazaId, vehicleType, tripType, amount, effectiveDate);
    }
}
