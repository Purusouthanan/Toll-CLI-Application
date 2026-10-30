package com.toll.module1.plaza.model;

import com.toll.common.PlazaStatus;
import com.toll.common.TripType;
import com.toll.common.VehicleType;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entity representing a Highway Toll Plaza.
 * Demonstrates:
 * - Encapsulation: State is strictly governed by business invariants.
 * - Business Rules:
 *   1. Must have at least 2 lanes.
 *   2. Cannot open until an active rate exists for EVERY vehicle type.
 *   3. Lane close rejected if vehicle is being processed.
 */
public class TollPlaza {
    private final int plazaId;
    private final String name;
    private final String highwayName;
    private final String location;
    private PlazaStatus status;
    private final List<Lane> lanes;
    private final List<TollRate> rates;

    public TollPlaza(int plazaId, String name, String highwayName, String location) {
        if (plazaId <= 0) throw new IllegalArgumentException("Plaza ID must be positive.");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Plaza name cannot be empty.");
        if (highwayName == null || highwayName.trim().isEmpty()) throw new IllegalArgumentException("Highway name cannot be empty.");
        if (location == null || location.trim().isEmpty()) throw new IllegalArgumentException("Location cannot be empty.");

        this.plazaId = plazaId;
        this.name = name.trim();
        this.highwayName = highwayName.trim();
        this.location = location.trim();
        this.status = PlazaStatus.INACTIVE; // New plazas start as INACTIVE until opened
        this.lanes = new CopyOnWriteArrayList<>();
        this.rates = new CopyOnWriteArrayList<>();
    }

    public int getPlazaId() {
        return plazaId;
    }

    public String getName() {
        return name;
    }

    public String getHighwayName() {
        return highwayName;
    }

    public String getLocation() {
        return location;
    }

    public synchronized PlazaStatus getStatus() {
        return status;
    }

    public List<Lane> getLanes() {
        return Collections.unmodifiableList(lanes);
    }

    public List<TollRate> getRates() {
        return Collections.unmodifiableList(rates);
    }

    public synchronized void addLane(Lane lane) {
        if (lane == null) throw new IllegalArgumentException("Lane cannot be null.");
        for (Lane existing : lanes) {
            if (existing.getLaneNumber() == lane.getLaneNumber()) {
                throw new IllegalArgumentException("Lane #" + lane.getLaneNumber() + " already exists in Plaza #" + plazaId);
            }
        }
        lanes.add(lane);
    }

    public Optional<Lane> findLane(int laneNumber) {
        return lanes.stream().filter(l -> l.getLaneNumber() == laneNumber).findFirst();
    }

    public synchronized void setRate(TollRate rate) {
        if (rate == null) throw new IllegalArgumentException("Rate cannot be null.");
        // Add or update rate for vehicle type + trip type + effective date
        rates.removeIf(r -> r.getVehicleType() == rate.getVehicleType() &&
                            r.getTripType() == rate.getTripType() &&
                            r.getEffectiveDate().isEqual(rate.getEffectiveDate()));
        rates.add(rate);
    }

    /**
     * Checks if active rates exist for every vehicle type as of the given date.
     */
    public synchronized boolean hasRatesForEveryVehicleType(LocalDate date) {
        for (VehicleType vt : VehicleType.values()) {
            boolean hasSingle = rates.stream().anyMatch(r -> r.getVehicleType() == vt &&
                    r.getTripType() == TripType.SINGLE &&
                    r.isEffectiveOn(date));
            if (!hasSingle) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to open the Toll Plaza.
     * Rule: Must have at least 2 lanes AND active rates for every vehicle type.
     */
    public synchronized boolean open(LocalDate date) {
        if (lanes.size() < 2) {
            return false; // Rejection: Minimum 2 lanes required
        }
        if (!hasRatesForEveryVehicleType(date)) {
            return false; // Rejection: Missing rates for some vehicle types
        }
        this.status = PlazaStatus.ACTIVE;
        return true;
    }

    public synchronized void close() {
        this.status = PlazaStatus.INACTIVE;
    }

    /**
     * Finds the applicable active rate for a vehicle type on a query date.
     * Picks the rate with the latest effectiveDate <= queryDate.
     */
    public Optional<Double> getActiveRate(VehicleType vehicleType, TripType tripType, LocalDate queryDate) {
        return rates.stream()
                .filter(r -> r.getVehicleType() == vehicleType &&
                             r.getTripType() == tripType &&
                             r.isEffectiveOn(queryDate))
                .max(Comparator.comparing(TollRate::getEffectiveDate))
                .map(TollRate::getAmount);
    }

    @Override
    public String toString() {
        return String.format("TollPlaza[%d: %s (%s, %s) | Status: %s | Lanes: %d | Rates: %d]",
                plazaId, name, highwayName, location, status, lanes.size(), rates.size());
    }
}
