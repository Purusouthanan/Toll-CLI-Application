package com.toll.module1.plaza.repository;

import com.toll.module1.plaza.model.TollPlaza;
import java.util.List;
import java.util.Optional;

public interface IPlazaRepository {
    void save(TollPlaza plaza);
    Optional<TollPlaza> findById(int plazaId);
    boolean existsById(int plazaId);
    List<TollPlaza> findAll();
    int count();
}
