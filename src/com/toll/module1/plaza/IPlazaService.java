package com.toll.module1.plaza;

import com.toll.common.VehicleType;
import java.time.LocalDate;

/**
 * Interface contract for Member 1: Module 1 (Toll Plaza and Lane Setup).
 * Responsibilities:
 * - Admin creates toll plazas and lanes (minimum 2 lanes per plaza).
 * - Lane types: FASTAG, CASH, EMERGENCY. Status: OPEN, CLOSED.
 * - Lane safety: A lane can be closed ONLY if no vehicle is currently being processed.
 * - Toll rates with effective dates per vehicle type and trip type (Single/Return).
 * - A plaza cannot open until a rate exists for EVERY vehicle type.
 */
public interface IPlazaService {
    // Toll Plaza operations
    void createPlaza(int plazaId, String name, String highwayName, String location);
    boolean openPlaza(int plazaId);
    void closePlaza(int plazaId);

    // Lane operations
    void addLane(int plazaId, int laneNumber, String laneType);
    boolean openLane(int plazaId, int laneNumber);
    boolean closeLane(int plazaId, int laneNumber);

    // Rate management with effective date
    void setTollRate(int plazaId, VehicleType vehicleType, String tripType, double amount, LocalDate effectiveDate);
    double getActiveRate(int plazaId, VehicleType vehicleType, String tripType, LocalDate queryDate);
}
