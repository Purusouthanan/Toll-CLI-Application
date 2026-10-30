package com.toll.module2.account.repository;

import com.toll.module2.account.model.FASTagAccount;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FASTag Account persistence operations.
 * Enforces strict 1:1 relationship between vehicle and FASTag account.
 */
public interface IFASTagAccountRepository {
    void save(FASTagAccount account);
    Optional<FASTagAccount> findByTagId(String tagId);
    Optional<FASTagAccount> findByVehicleRegistration(String registrationNumber);
    boolean existsByTagId(String tagId);
    boolean existsByVehicleRegistration(String registrationNumber);
    List<FASTagAccount> findAll();
    void delete(String tagId);
    int count();
}
