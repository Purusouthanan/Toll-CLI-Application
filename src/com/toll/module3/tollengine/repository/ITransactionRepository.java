package com.toll.module3.tollengine.repository;

import com.toll.module3.tollengine.model.TollTransaction;
import java.util.List;
import java.util.Optional;

public interface ITransactionRepository {
    void save(TollTransaction transaction);
    Optional<TollTransaction> findById(String transactionId);
    List<TollTransaction> findAll();
    List<TollTransaction> findByVehicle(String vehicleRegNo);
    List<TollTransaction> findByTagId(String tagId);
    List<TollTransaction> findByPlaza(int plazaId);
    int count();
}
