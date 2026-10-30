package com.toll.module2.account.service;

import com.toll.common.VehicleType;
import com.toll.module2.account.model.FASTagAccount;
import com.toll.module2.account.model.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for Vehicle & FASTag Account Management (Module 2).
 * Demonstrates Abstraction: Exposes high-level contracts while concealing in-memory repository implementation details.
 */
public interface IAccountService {

    // --- Vehicle Management ---
    Vehicle registerVehicle(VehicleType type, String registrationNumber, String ownerName, String phoneNumber);
    Vehicle registerMultiAxleVehicle(String registrationNumber, String ownerName, String phoneNumber, int axles);
    Optional<Vehicle> findVehicleByRegistration(String registrationNumber);
    List<Vehicle> getAllVehicles();

    // --- FASTag Account Lifecycle & 1:1 Association ---
    FASTagAccount issueFASTag(String registrationNumber, String tagId, double initialDeposit);
    Optional<FASTagAccount> findAccountByTagId(String tagId);
    Optional<FASTagAccount> findAccountByVehicleRegistration(String registrationNumber);
    List<FASTagAccount> getAllAccounts();

    // --- Financial Operations & Rules ---
    RechargeResult rechargeAccount(String tagId, double amount);
    DebitResult deductToll(String tagId, double amount);
    boolean refundAccount(String tagId, double amount, String disputeReason);

    // --- Administrative Controls ---
    void blacklistTag(String tagId, String reason);
    void unblacklistTag(String tagId);
    void closeAccount(String tagId);

    // --- Fast Cross-Module Validation (Used by Module 3: Toll Engine) ---
    boolean isTagValidAndActive(String tagId);
    boolean isTagEligibleForToll(String tagId, double tollAmount);
}
