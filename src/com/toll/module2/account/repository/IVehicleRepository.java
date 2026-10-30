package com.toll.module2.account.repository;

import com.toll.module2.account.model.Vehicle;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Vehicle persistence operations.
 * Demonstrates Abstraction for in-memory data store.
 */
public interface IVehicleRepository {
    void save(Vehicle vehicle);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumber(String registrationNumber);
    List<Vehicle> findAll();
    void delete(String registrationNumber);
    int count();
}
