package com.toll.module4.pricing.repository;

import com.toll.module4.pricing.model.MonthlyPass;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IPassRepository {
    void save(MonthlyPass pass);
    Optional<MonthlyPass> findById(String passId);
    Optional<MonthlyPass> findActivePass(String vehicleRegNo, int plazaId, LocalDate date);
    List<MonthlyPass> findAll();
    int count();
}
