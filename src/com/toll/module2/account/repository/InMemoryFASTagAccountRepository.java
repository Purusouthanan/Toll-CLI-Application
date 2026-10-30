package com.toll.module2.account.repository;

import com.toll.module2.account.exception.DuplicateTagException;
import com.toll.module2.account.exception.VehicleAlreadyHasTagException;
import com.toll.module2.account.model.FASTagAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe In-Memory implementation of IFASTagAccountRepository.
 * Maintains primary key index (tagId) and unique secondary index (vehicleRegistrationNumber)
 * to strictly enforce the 1:1 vehicle-to-tag relationship.
 */
public class InMemoryFASTagAccountRepository implements IFASTagAccountRepository {
    private final Map<String, FASTagAccount> tagStore = new ConcurrentHashMap<>();
    private final Map<String, String> vehicleToTagIndex = new ConcurrentHashMap<>();

    @Override
    public synchronized void save(FASTagAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("Cannot save a null FASTag account.");
        }
        String tagId = account.getTagId();
        String vehicleReg = account.getVehicleRegistrationNumber();

        // Check if another tag already exists for this vehicle
        String existingTagForVehicle = vehicleToTagIndex.get(vehicleReg);
        if (existingTagForVehicle != null && !existingTagForVehicle.equalsIgnoreCase(tagId)) {
            throw new VehicleAlreadyHasTagException(vehicleReg, existingTagForVehicle);
        }

        tagStore.put(tagId, account);
        vehicleToTagIndex.put(vehicleReg, tagId);
    }

    @Override
    public Optional<FASTagAccount> findByTagId(String tagId) {
        if (tagId == null) return Optional.empty();
        return Optional.ofNullable(tagStore.get(tagId.trim().toUpperCase()));
    }

    @Override
    public Optional<FASTagAccount> findByVehicleRegistration(String registrationNumber) {
        if (registrationNumber == null) return Optional.empty();
        String tagId = vehicleToTagIndex.get(registrationNumber.trim().toUpperCase());
        if (tagId == null) return Optional.empty();
        return findByTagId(tagId);
    }

    @Override
    public boolean existsByTagId(String tagId) {
        if (tagId == null) return false;
        return tagStore.containsKey(tagId.trim().toUpperCase());
    }

    @Override
    public boolean existsByVehicleRegistration(String registrationNumber) {
        if (registrationNumber == null) return false;
        return vehicleToTagIndex.containsKey(registrationNumber.trim().toUpperCase());
    }

    @Override
    public List<FASTagAccount> findAll() {
        return new ArrayList<>(tagStore.values());
    }

    @Override
    public synchronized void delete(String tagId) {
        if (tagId == null) return;
        FASTagAccount account = tagStore.remove(tagId.trim().toUpperCase());
        if (account != null) {
            vehicleToTagIndex.remove(account.getVehicleRegistrationNumber());
        }
    }

    @Override
    public int count() {
        return tagStore.size();
    }
}
