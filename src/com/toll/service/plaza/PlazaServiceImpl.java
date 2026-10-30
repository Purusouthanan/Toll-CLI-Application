package com.toll.service.plaza;

import com.toll.exception.TollManagementException;
import com.toll.exception.UnauthorizedException;
import com.toll.model.admin.Admin;
import com.toll.model.enums.TripType;
import com.toll.model.enums.VehicleType;
import com.toll.model.plaza.Lane;
import com.toll.model.plaza.TollPlaza;
import com.toll.model.plaza.TollRate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete implementation of IPlazaService managing Toll Plazas, Lanes, and Rates in memory.
 * Demonstrates Module 1 business logic enforcement and Thread Safety.
 */
public class PlazaServiceImpl implements IPlazaService {

    private final Map<Integer, TollPlaza> plazaStore = new ConcurrentHashMap<>();
    private final AtomicInteger plazaIdGenerator = new AtomicInteger(100);

    /**
     * Helper to assert that the caller is an active, authorized Admin.
     */
    private void validateAdmin(Admin admin, String operationDescription) {
        if (admin == null || !admin.isActive()) {
            throw new UnauthorizedException(
                    "Unauthorized Action: Only an authorized Admin can " + operationDescription + "."
            );
        }
    }

    @Override
    public TollPlaza createPlaza(Admin admin, String name, String highwayName, String location) {
        validateAdmin(admin, "create toll plazas");

        int plazaId = plazaIdGenerator.incrementAndGet();
        TollPlaza plaza = new TollPlaza(plazaId, name, highwayName, location);
        plazaStore.put(plazaId, plaza);
        return plaza;
    }

    @Override
    public void addLane(Admin admin, int plazaId, Lane lane) {
        validateAdmin(admin, "create and add lanes");

        TollPlaza plaza = getPlaza(plazaId);
        plaza.addLane(lane);
    }

    @Override
    public TollRate setTollRate(Admin admin, int plazaId, VehicleType vehicleType, TripType tripType, double amount, LocalDate effectiveDate) {
        validateAdmin(admin, "configure toll rates");

        TollPlaza plaza = getPlaza(plazaId);
        TollRate rate = new TollRate(plazaId, vehicleType, tripType, amount, effectiveDate);
        plaza.addTollRate(rate);
        return rate;
    }

    @Override
    public void openPlaza(Admin admin, int plazaId) {
        validateAdmin(admin, "open toll plazas");

        TollPlaza plaza = getPlaza(plazaId);
        // Will throw PlazaNotReadyException if < 2 lanes or missing any vehicle type rate
        plaza.openPlaza();
    }

    @Override
    public void closePlaza(Admin admin, int plazaId) {
        validateAdmin(admin, "close toll plazas");

        TollPlaza plaza = getPlaza(plazaId);
        plaza.closePlaza();
    }

    @Override
    public void openLane(Admin admin, int plazaId, int laneNumber) {
        validateAdmin(admin, "open lanes");

        TollPlaza plaza = getPlaza(plazaId);
        Lane lane = plaza.getLane(laneNumber);
        lane.openLane();
    }

    @Override
    public void closeLane(Admin admin, int plazaId, int laneNumber) {
        validateAdmin(admin, "close lanes");

        TollPlaza plaza = getPlaza(plazaId);
        Lane lane = plaza.getLane(laneNumber);
        // Will throw LaneOperationException if a vehicle is currently being processed
        lane.closeLane();
    }

    @Override
    public TollRate getEffectiveRate(int plazaId, VehicleType vehicleType, TripType tripType, LocalDate queryDate) {
        TollPlaza plaza = getPlaza(plazaId);
        return plaza.getEffectiveRate(vehicleType, tripType, queryDate);
    }

    @Override
    public TollPlaza getPlaza(int plazaId) {
        TollPlaza plaza = plazaStore.get(plazaId);
        if (plaza == null) {
            throw new TollManagementException("Toll Plaza with ID " + plazaId + " not found.");
        }
        return plaza;
    }

    @Override
    public List<TollPlaza> getAllPlazas() {
        return Collections.unmodifiableList(new ArrayList<>(plazaStore.values()));
    }
}
