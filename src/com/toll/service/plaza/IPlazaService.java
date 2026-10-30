package com.toll.service.plaza;

import com.toll.model.admin.Admin;
import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;
import com.toll.model.plaza.Lane;
import com.toll.model.plaza.TollPlaza;
import com.toll.model.plaza.TollRate;

import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for Toll Plaza and Lane Setup (Module 1).
 * Demonstrates Abstraction by hiding in-memory persistence and validation complexity.
 */
public interface IPlazaService {

    /**
     * Creates a new Toll Plaza.
     * Business Rule: Only an Admin can create toll plazas.
     */
    TollPlaza createPlaza(Admin admin, String name, String highwayName, String location);

    /**
     * Adds a lane to a designated toll plaza.
     * Business Rule: Only an Admin can create lanes.
     */
    void addLane(Admin admin, int plazaId, Lane lane);

    /**
     * Sets a toll rate for a plaza, vehicle type, and trip type with an effective date.
     * Business Rule: Only an Admin can configure toll rates.
     */
    TollRate setTollRate(Admin admin, int plazaId, VehicleType vehicleType, TripType tripType, double amount, LocalDate effectiveDate);

    /**
     * Opens a toll plaza for public traffic.
     * Business Rules:
     * - Only an Admin can open a plaza.
     * - Must have at least 2 lanes.
     * - A plaza cannot open until a rate exists for every vehicle type.
     */
    void openPlaza(Admin admin, int plazaId);

    /**
     * Closes a toll plaza.
     */
    void closePlaza(Admin admin, int plazaId);

    /**
     * Opens an individual lane.
     */
    void openLane(Admin admin, int plazaId, int laneNumber);

    /**
     * Closes an individual lane.
     * Business Rule: A lane can be closed only if no vehicle is being processed in it.
     */
    void closeLane(Admin admin, int plazaId, int laneNumber);

    /**
     * Retrieves the active effective rate for a vehicle crossing on a specific date.
     * Business Rule: A rate change applies only from its effective date. Earlier transactions are not affected.
     */
    TollRate getEffectiveRate(int plazaId, VehicleType vehicleType, TripType tripType, LocalDate queryDate);

    /**
     * Retrieves a plaza by ID.
     */
    TollPlaza getPlaza(int plazaId);

    /**
     * Retrieves all plazas.
     */
    List<TollPlaza> getAllPlazas();
}
