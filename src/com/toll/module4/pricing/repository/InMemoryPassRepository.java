package com.toll.module4.pricing.repository;

import com.toll.module4.pricing.model.MonthlyPass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for MonthlyPass.
 * Enforces rule: A vehicle can have only one valid pass per plaza at a time.
 */
public class InMemoryPassRepository implements IPassRepository {
    private final Map<String, MonthlyPass> passStore = new ConcurrentHashMap<>();

    @Override
    public synchronized void save(MonthlyPass pass) {
        if (pass == null) throw new IllegalArgumentException("Pass cannot be null.");

        // Check if vehicle already has an active overlapping pass for this plaza
        for (MonthlyPass existing : passStore.values()) {
            if (existing.getVehicleRegistrationNumber().equalsIgnoreCase(pass.getVehicleRegistrationNumber()) &&
                existing.getPlazaId() == pass.getPlazaId() &&
                !existing.getPassId().equalsIgnoreCase(pass.getPassId())) {

                // Check date overlap
                boolean overlaps = !(pass.getEndDate().isBefore(existing.getStartDate()) ||
                                     pass.getStartDate().isAfter(existing.getEndDate()));
                if (overlaps) {
                    throw new IllegalStateException(String.format(
                            "Vehicle '%s' already holds an active pass (%s) for Plaza #%d in this date range.",
                            pass.getVehicleRegistrationNumber(), existing.getPassId(), pass.getPlazaId()));
                }
            }
        }
        passStore.put(pass.getPassId(), pass);
    }

    @Override
    public Optional<MonthlyPass> findById(String passId) {
        if (passId == null) return Optional.empty();
        return Optional.ofNullable(passStore.get(passId.trim().toUpperCase()));
    }

    @Override
    public Optional<MonthlyPass> findActivePass(String vehicleRegNo, int plazaId, LocalDate date) {
        if (vehicleRegNo == null || date == null) return Optional.empty();
        return passStore.values().stream()
                .filter(p -> p.getVehicleRegistrationNumber().equalsIgnoreCase(vehicleRegNo.trim()) &&
                             p.getPlazaId() == plazaId &&
                             p.isValidOn(date))
                .findFirst();
    }

    @Override
    public List<MonthlyPass> findAll() {
        return new ArrayList<>(passStore.values());
    }

    @Override
    public int count() {
        return passStore.size();
    }
}
