package com.toll.model.plaza;

import com.toll.exception.PlazaNotReadyException;
import com.toll.exception.TollManagementException;
import com.toll.model.enums.PlazaStatus;
import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entity representing an Indian Highway Toll Plaza.
 * Demonstrates Encapsulation and Domain Integrity Enforcements.
 */
public class TollPlaza {
    private final int plazaId;
    private String name;
    private String highwayName;
    private String location;
    private PlazaStatus status;

    // Thread-safe collections for lanes and rates
    private final Map<Integer, Lane> lanes = new ConcurrentHashMap<>();
    private final List<TollRate> tollRates = new CopyOnWriteArrayList<>();

    public TollPlaza(int plazaId, String name, String highwayName, String location) {
        if (plazaId <= 0) {
            throw new IllegalArgumentException("Plaza ID must be positive.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plaza name cannot be empty.");
        }
        if (highwayName == null || highwayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Highway name cannot be empty.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty.");
        }

        this.plazaId = plazaId;
        this.name = name;
        this.highwayName = highwayName;
        this.location = location;
        this.status = PlazaStatus.INACTIVE; // New plazas start as INACTIVE until opened by Admin
    }

    public int getPlazaId() {
        return plazaId;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized String getHighwayName() {
        return highwayName;
    }

    public synchronized void setHighwayName(String highwayName) {
        this.highwayName = highwayName;
    }

    public synchronized String getLocation() {
        return location;
    }

    public synchronized void setLocation(String location) {
        this.location = location;
    }

    public synchronized PlazaStatus getStatus() {
        return status;
    }

    public synchronized boolean isOpen() {
        return this.status == PlazaStatus.ACTIVE;
    }

    /**
     * Adds a lane to this toll plaza.
     */
    public synchronized void addLane(Lane lane) {
        if (lane == null) {
            throw new IllegalArgumentException("Lane cannot be null.");
        }
        if (lanes.containsKey(lane.getLaneNumber())) {
            throw new IllegalArgumentException("Lane number " + lane.getLaneNumber() + " already exists in plaza " + name);
        }
        lanes.put(lane.getLaneNumber(), lane);
    }

    /**
     * Retrieves a specific lane by its number.
     */
    public synchronized Lane getLane(int laneNumber) {
        Lane lane = lanes.get(laneNumber);
        if (lane == null) {
            throw new TollManagementException("Lane #" + laneNumber + " does not exist at Plaza '" + name + "'.");
        }
        return lane;
    }

    /**
     * Returns an unmodifiable list of all lanes in this plaza.
     */
    public synchronized List<Lane> getLanes() {
        return Collections.unmodifiableList(new ArrayList<>(lanes.values()));
    }

    /**
     * Adds or updates a toll rate.
     */
    public synchronized void addTollRate(TollRate rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Toll rate cannot be null.");
        }
        tollRates.add(rate);
    }

    /**
     * Returns an unmodifiable list of all configured toll rates.
     */
    public synchronized List<TollRate> getTollRates() {
        return Collections.unmodifiableList(new ArrayList<>(tollRates));
    }

    /**
     * Validates whether this plaza fulfills all business requirements to open:
     * 1. Must have at least 2 lanes.
     * 2. Must have toll rates configured for every VehicleType (for both Single and Return trip types).
     */
    public synchronized List<String> getReadinessDeficiencies() {
        List<String> deficiencies = new ArrayList<>();

        if (lanes.size() < 2) {
            deficiencies.add("Plaza has only " + lanes.size() + " lane(s). Minimum 2 lanes are required.");
        }

        Set<VehicleType> coveredVehicleTypes = new HashSet<>();
        for (TollRate rate : tollRates) {
            coveredVehicleTypes.add(rate.getVehicleType());
        }

        for (VehicleType vt : VehicleType.values()) {
            if (!coveredVehicleTypes.contains(vt)) {
                deficiencies.add("Missing toll rate for vehicle category: " + vt.name() + " (" + vt.getDescription() + ").");
            }
        }

        return deficiencies;
    }

    /**
     * Checks if the plaza is ready to open.
     */
    public synchronized boolean isReadyToOpen() {
        return getReadinessDeficiencies().isEmpty();
    }

    /**
     * Opens the plaza for public traffic.
     * Enforces the business rules:
     * - Must have at least 2 lanes.
     * - A plaza cannot open until a rate exists for every vehicle type.
     */
    public synchronized void openPlaza() {
        List<String> deficiencies = getReadinessDeficiencies();
        if (!deficiencies.isEmpty()) {
            throw new PlazaNotReadyException(
                    "Cannot open Plaza '" + name + "'. Violations found:\n - " + String.join("\n - ", deficiencies)
            );
        }
        this.status = PlazaStatus.ACTIVE;
    }

    /**
     * Closes the toll plaza.
     */
    public synchronized void closePlaza() {
        this.status = PlazaStatus.INACTIVE;
    }

    /**
     * Finds the applicable effective toll rate for a vehicle type, trip type, and query date.
     * Business Rule: A rate change applies only from its effective date. Earlier transactions are not affected.
     */
    public synchronized TollRate getEffectiveRate(VehicleType vehicleType, TripType tripType, LocalDate queryDate) {
        if (vehicleType == null || tripType == null || queryDate == null) {
            throw new IllegalArgumentException("Vehicle type, trip type, and query date must be non-null.");
        }

        return tollRates.stream()
                .filter(r -> r.getVehicleType() == vehicleType && r.getTripType() == tripType)
                .filter(r -> r.isApplicableOn(queryDate))
                .max(Comparator.comparing(TollRate::getEffectiveDate))
                .orElseThrow(() -> new TollManagementException(
                        String.format("No effective toll rate found for Plaza '%s' [Vehicle: %s, Trip: %s, Date: %s]",
                                name, vehicleType, tripType, queryDate)
                ));
    }

    @Override
    public String toString() {
        return String.format("TollPlaza[ID: %d | Name: '%s' | Highway: '%s' | Location: '%s' | Status: %s | Lanes: %d | Rates: %d]",
                plazaId, name, highwayName, location, status, lanes.size(), tollRates.size());
    }
}
