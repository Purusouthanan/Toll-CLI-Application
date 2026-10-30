package com.toll.module1.plaza;

import com.toll.common.TripType;
import com.toll.common.VehicleType;
import com.toll.module1.plaza.model.*;
import com.toll.module1.plaza.repository.IPlazaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of IPlazaService for Module 1.
 * Business Rules:
 * 1. Admin can create toll plazas and lanes.
 * 2. Plaza has at least 2 lanes.
 * 3. Plaza cannot open until an active rate exists for EVERY vehicle type.
 * 4. A lane can be closed only if no vehicle is being processed in it.
 * 5. Rate changes apply only from their effective date without affecting past transactions.
 */
public class PlazaServiceImpl implements IPlazaService {
    private final IPlazaRepository plazaRepository;

    public PlazaServiceImpl(IPlazaRepository plazaRepository) {
        if (plazaRepository == null) throw new IllegalArgumentException("Plaza repository cannot be null.");
        this.plazaRepository = plazaRepository;
    }

    @Override
    public TollPlaza createPlaza(int plazaId, String name, String highwayName, String location) {
        if (plazaRepository.existsById(plazaId)) {
            throw new IllegalArgumentException("Toll Plaza with ID " + plazaId + " already exists.");
        }
        TollPlaza plaza = new TollPlaza(plazaId, name, highwayName, location);
        plazaRepository.save(plaza);
        return plaza;
    }

    @Override
    public boolean openPlaza(int plazaId) {
        return openPlaza(plazaId, LocalDate.now());
    }

    @Override
    public boolean openPlaza(int plazaId, LocalDate currentDate) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));
        return plaza.open(currentDate);
    }

    @Override
    public void closePlaza(int plazaId) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));
        plaza.close();
    }

    @Override
    public Optional<TollPlaza> getPlaza(int plazaId) {
        return plazaRepository.findById(plazaId);
    }

    @Override
    public List<TollPlaza> getAllPlazas() {
        return plazaRepository.findAll();
    }

    @Override
    public void addLane(int plazaId, int laneNumber, String laneTypeStr) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));

        Lane lane;
        String typeUpper = laneTypeStr.trim().toUpperCase();
        if (typeUpper.contains("FAST") || typeUpper.equals("FASTAG")) {
            lane = new FASTagLane(laneNumber);
        } else if (typeUpper.contains("EMERGENCY")) {
            lane = new EmergencyLane(laneNumber);
        } else {
            lane = new CashLane(laneNumber);
        }
        plaza.addLane(lane);
    }

    @Override
    public boolean openLane(int plazaId, int laneNumber) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));
        Lane lane = plaza.findLane(laneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lane #" + laneNumber + " not found in Plaza #" + plazaId));
        lane.open();
        return true;
    }

    @Override
    public boolean closeLane(int plazaId, int laneNumber) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));
        Lane lane = plaza.findLane(laneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lane #" + laneNumber + " not found in Plaza #" + plazaId));
        return lane.close(); // Safety close rule checked here!
    }

    @Override
    public void setTollRate(int plazaId, VehicleType vehicleType, TripType tripType, double amount, LocalDate effectiveDate) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));
        TollRate rate = new TollRate(plazaId, vehicleType, tripType, amount, effectiveDate);
        plaza.setRate(rate);
    }

    @Override
    public double getActiveRate(int plazaId, VehicleType vehicleType, TripType tripType, LocalDate queryDate) {
        TollPlaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza #" + plazaId + " not found."));
        return plaza.getActiveRate(vehicleType, tripType, queryDate)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No active rate found for %s (%s) at Plaza #%d on date %s",
                                vehicleType, tripType, plazaId, queryDate)));
    }
}
