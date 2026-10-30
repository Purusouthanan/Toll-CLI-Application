package com.toll.module1.plaza.model;

import com.toll.common.TripType;
import com.toll.common.VehicleType;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing a Toll Rate configuration.
 * A rate is effective only on or after its effectiveDate.
 */
public class TollRate {
    private final int plazaId;
    private final VehicleType vehicleType;
    private final TripType tripType;
    private final double amount;
    private final LocalDate effectiveDate;

    public TollRate(int plazaId, VehicleType vehicleType, TripType tripType, double amount, LocalDate effectiveDate) {
        if (vehicleType == null) throw new IllegalArgumentException("Vehicle type cannot be null.");
        if (tripType == null) throw new IllegalArgumentException("Trip type cannot be null.");
        if (amount < 0.0) throw new IllegalArgumentException("Toll amount cannot be negative.");
        if (effectiveDate == null) throw new IllegalArgumentException("Effective date cannot be null.");

        this.plazaId = plazaId;
        this.vehicleType = vehicleType;
        this.tripType = tripType;
        this.amount = amount;
        this.effectiveDate = effectiveDate;
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

    public boolean isEffectiveOn(LocalDate date) {
        return !effectiveDate.isAfter(date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TollRate tollRate = (TollRate) o;
        return plazaId == tollRate.plazaId &&
                Double.compare(amount, tollRate.amount) == 0 &&
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
        return String.format("Rate[Plaza %d | %s | %s: Rs. %.2f (Eff: %s)]",
                plazaId, vehicleType, tripType, amount, effectiveDate);
    }
}
