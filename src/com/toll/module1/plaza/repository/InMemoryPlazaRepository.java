package com.toll.module1.plaza.repository;

import com.toll.module1.plaza.model.TollPlaza;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPlazaRepository implements IPlazaRepository {
    private final Map<Integer, TollPlaza> plazaStore = new ConcurrentHashMap<>();

    @Override
    public void save(TollPlaza plaza) {
        if (plaza == null) throw new IllegalArgumentException("Plaza cannot be null.");
        plazaStore.put(plaza.getPlazaId(), plaza);
    }

    @Override
    public Optional<TollPlaza> findById(int plazaId) {
        return Optional.ofNullable(plazaStore.get(plazaId));
    }

    @Override
    public boolean existsById(int plazaId) {
        return plazaStore.containsKey(plazaId);
    }

    @Override
    public List<TollPlaza> findAll() {
        return new ArrayList<>(plazaStore.values());
    }

    @Override
    public int count() {
        return plazaStore.size();
    }
}
