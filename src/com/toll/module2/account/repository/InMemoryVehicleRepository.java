package com.toll.module2.account.repository;

import com.toll.module2.account.model.Vehicle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe In-Memory implementation of IVehicleRepository.
 * Uses ConcurrentHashMap for lock-free reads and atomic operations.
 */
public class InMemoryVehicleRepository implements IVehicleRepository {
    private final Map<String, Vehicle> vehicleStore = new ConcurrentHashMap<>();

    @Override
    public void save(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Cannot save a null vehicle.");
        }
        vehicleStore.put(vehicle.getRegistrationNumber(), vehicle);
    }

    @Override
    public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) return Optional.empty();
        return Optional.ofNullable(vehicleStore.get(registrationNumber.trim().toUpperCase()));
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null) return false;
        return vehicleStore.containsKey(registrationNumber.trim().toUpperCase());
    }

    @Override
    public List<Vehicle> findAll() {
        return new ArrayList<>(vehicleStore.values());
    }

    @Override
    public void delete(String registrationNumber) {
        if (registrationNumber != null) {
            vehicleStore.remove(registrationNumber.trim().toUpperCase());
        }
    }

    @Override
    public int count() {
        return vehicleStore.size();
    }
}
