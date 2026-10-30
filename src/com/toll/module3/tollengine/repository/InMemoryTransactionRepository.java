package com.toll.module3.tollengine.repository;

import com.toll.module3.tollengine.model.TollTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionRepository implements ITransactionRepository {
    private final Map<String, TollTransaction> store = new ConcurrentHashMap<>();

    @Override
    public void save(TollTransaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("Transaction cannot be null.");
        store.put(transaction.transactionId(), transaction);
    }

    @Override
    public Optional<TollTransaction> findById(String transactionId) {
        if (transactionId == null) return Optional.empty();
        return Optional.ofNullable(store.get(transactionId));
    }

    @Override
    public List<TollTransaction> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<TollTransaction> findByVehicle(String vehicleRegNo) {
        if (vehicleRegNo == null) return List.of();
        return store.values().stream()
                .filter(t -> vehicleRegNo.equalsIgnoreCase(t.vehicleRegNo()))
                .toList();
    }

    @Override
    public List<TollTransaction> findByTagId(String tagId) {
        if (tagId == null) return List.of();
        return store.values().stream()
                .filter(t -> tagId.equalsIgnoreCase(t.tagId()))
                .toList();
    }

    @Override
    public List<TollTransaction> findByPlaza(int plazaId) {
        return store.values().stream()
                .filter(t -> t.plazaId() == plazaId)
                .toList();
    }

    @Override
    public int count() {
        return store.size();
    }
}
