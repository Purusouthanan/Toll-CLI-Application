package com.toll.module1.plaza;

import com.toll.common.TripType;
import com.toll.common.VehicleType;
import com.toll.module1.plaza.model.TollPlaza;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interface contract for Module 1 (Toll Plaza and Lane Setup).
 */
public interface IPlazaService {
    // Toll Plaza operations
    TollPlaza createPlaza(int plazaId, String name, String highwayName, String location);
    boolean openPlaza(int plazaId);
    boolean openPlaza(int plazaId, LocalDate currentDate);
    void closePlaza(int plazaId);
    Optional<TollPlaza> getPlaza(int plazaId);
    List<TollPlaza> getAllPlazas();

    // Lane operations
    void addLane(int plazaId, int laneNumber, String laneType);
    boolean openLane(int plazaId, int laneNumber);
    boolean closeLane(int plazaId, int laneNumber);

    // Rate management with effective date
    void setTollRate(int plazaId, VehicleType vehicleType, TripType tripType, double amount, LocalDate effectiveDate);
    double getActiveRate(int plazaId, VehicleType vehicleType, TripType tripType, LocalDate queryDate);
}
