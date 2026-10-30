package com.toll.module2.account.service;

import com.toll.common.AccountStatus;
import com.toll.common.VehicleType;
import com.toll.module2.account.exception.*;
import com.toll.module2.account.model.*;
import com.toll.module2.account.repository.IFASTagAccountRepository;
import com.toll.module2.account.repository.IVehicleRepository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of IAccountService for Module 2.
 * Fully enforces Indian NHAI FASTag Business Rules:
 * 1. Unique vehicle registration number.
 * 2. 1:1 Tag-to-Vehicle link. A vehicle can have only one active tag.
 * 3. Tag status: ACTIVE, BLOCKED, or CLOSED.
 * 4. Auto-block when balance < Rs. 100.00.
 * 5. Minimum recharge Rs. 100.00, maximum recharge Rs. 10,000.00.
 * 6. Maximum account balance cap Rs. 50,000.00. Balance can NEVER be negative.
 * 7. Auto-unblock when recharge takes balance >= Rs. 100.00.
 * 8. Admin blacklisting: Blacklisted tags are refused at every toll plaza.
 */
public class AccountServiceImpl implements IAccountService {
    private final IVehicleRepository vehicleRepository;
    private final IFASTagAccountRepository accountRepository;

    public AccountServiceImpl(IVehicleRepository vehicleRepository, IFASTagAccountRepository accountRepository) {
        if (vehicleRepository == null || accountRepository == null) {
            throw new IllegalArgumentException("Repositories cannot be null.");
        }
        this.vehicleRepository = vehicleRepository;
        this.accountRepository = accountRepository;
    }

    // --- Vehicle Operations ---

    @Override
    public Vehicle registerVehicle(VehicleType type, String registrationNumber, String ownerName, String phoneNumber) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number cannot be empty.");
        }
        String formattedReg = registrationNumber.trim().toUpperCase();
        if (vehicleRepository.existsByRegistrationNumber(formattedReg)) {
            throw new DuplicateVehicleException(formattedReg);
        }

        Vehicle vehicle = VehicleFactory.createVehicle(type, formattedReg, ownerName, phoneNumber);
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    @Override
    public Vehicle registerMultiAxleVehicle(String registrationNumber, String ownerName, String phoneNumber, int axles) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number cannot be empty.");
        }
        String formattedReg = registrationNumber.trim().toUpperCase();
        if (vehicleRepository.existsByRegistrationNumber(formattedReg)) {
            throw new DuplicateVehicleException(formattedReg);
        }

        Vehicle vehicle = VehicleFactory.createMultiAxle(formattedReg, ownerName, phoneNumber, axles);
        vehicleRepository.save(vehicle);
        return vehicle;
    }

    @Override
    public Optional<Vehicle> findVehicleByRegistration(String registrationNumber) {
        if (registrationNumber == null) return Optional.empty();
        return vehicleRepository.findByRegistrationNumber(registrationNumber.trim().toUpperCase());
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    // --- FASTag Account Lifecycle & 1:1 Association ---

    @Override
    public FASTagAccount issueFASTag(String registrationNumber, String tagId, double initialDeposit) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be empty.");
        }
        if (tagId == null || tagId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag ID cannot be empty.");
        }

        String formattedReg = registrationNumber.trim().toUpperCase();
        String formattedTag = tagId.trim().toUpperCase();

        // 1. Verify vehicle is registered
        if (!vehicleRepository.existsByRegistrationNumber(formattedReg)) {
            throw new VehicleNotFoundException(formattedReg);
        }

        // 2. Verify Tag ID is globally unique
        if (accountRepository.existsByTagId(formattedTag)) {
            throw new DuplicateTagException(formattedTag);
        }

        // 3. Verify vehicle does not already have an active/existing tag (1:1 constraint)
        Optional<FASTagAccount> existingTagOpt = accountRepository.findByVehicleRegistration(formattedReg);
        if (existingTagOpt.isPresent()) {
            FASTagAccount existingTag = existingTagOpt.get();
            if (existingTag.getStatus() != AccountStatus.CLOSED) {
                throw new VehicleAlreadyHasTagException(formattedReg, existingTag.getTagId());
            }
        }

        FASTagAccount account = new FASTagAccount(formattedTag, formattedReg, initialDeposit);
        accountRepository.save(account);
        return account;
    }

    @Override
    public Optional<FASTagAccount> findAccountByTagId(String tagId) {
        if (tagId == null) return Optional.empty();
        return accountRepository.findByTagId(tagId.trim().toUpperCase());
    }

    @Override
    public Optional<FASTagAccount> findAccountByVehicleRegistration(String registrationNumber) {
        if (registrationNumber == null) return Optional.empty();
        return accountRepository.findByVehicleRegistration(registrationNumber.trim().toUpperCase());
    }

    @Override
    public List<FASTagAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    // --- Financial Operations ---

    @Override
    public RechargeResult rechargeAccount(String tagId, double amount) {
        if (tagId == null) {
            return RechargeResult.failure("UNKNOWN", amount, 0.0, "Tag ID cannot be null.");
        }
        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(formattedTag);
        if (accountOpt.isEmpty()) {
            return RechargeResult.failure(formattedTag, amount, 0.0, "FASTag account not found: " + formattedTag);
        }

        FASTagAccount account = accountOpt.get();
        try {
            boolean autoUnblocked = account.recharge(amount);
            return RechargeResult.success(formattedTag, amount, account.getBalance(), autoUnblocked);
        } catch (FASTagAccountException e) {
            return RechargeResult.failure(formattedTag, amount, account.getBalance(), e.getMessage());
        }
    }

    @Override
    public DebitResult deductToll(String tagId, double amount) {
        if (tagId == null) {
            return DebitResult.failure("UNKNOWN", amount, 0.0, "Tag ID cannot be null.");
        }
        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(formattedTag);
        if (accountOpt.isEmpty()) {
            return DebitResult.failure(formattedTag, amount, 0.0, "FASTag account not found: " + formattedTag);
        }

        FASTagAccount account = accountOpt.get();
        double currentBal = account.getBalance();

        // Check blacklist refusal
        if (account.isBlacklisted()) {
            return DebitResult.failure(formattedTag, amount, currentBal,
                    "REFUSED AT PLAZA: FASTag is BLACKLISTED (" + account.getBlacklistReason() + ")");
        }

        // Check status
        if (account.getStatus() == AccountStatus.CLOSED) {
            return DebitResult.failure(formattedTag, amount, currentBal, "REFUSED: FASTag account is CLOSED.");
        }
        if (account.getStatus() == AccountStatus.BLOCKED) {
            return DebitResult.failure(formattedTag, amount, currentBal,
                    String.format("REFUSED: FASTag is BLOCKED (Balance Rs. %.2f < Rs. 100.00). Divert to cash lane.", currentBal));
        }

        // Check balance sufficiency
        if (currentBal - amount < 0.0) {
            return DebitResult.failure(formattedTag, amount, currentBal,
                    String.format("REFUSED: Insufficient balance (Available: Rs. %.2f, Required: Rs. %.2f). Divert to cash lane.",
                            currentBal, amount));
        }

        try {
            boolean autoBlocked = account.debit(amount);
            double newBalance = account.getBalance();
            boolean lowBalanceAlert = newBalance < FASTagAccount.LOW_BALANCE_ALERT_THRESHOLD;
            return DebitResult.success(formattedTag, amount, newBalance, autoBlocked, lowBalanceAlert);
        } catch (FASTagAccountException e) {
            return DebitResult.failure(formattedTag, amount, account.getBalance(), e.getMessage());
        }
    }

    @Override
    public boolean refundAccount(String tagId, double amount, String disputeReason) {
        if (tagId == null) return false;
        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(formattedTag);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException(formattedTag);
        }
        FASTagAccount account = accountOpt.get();
        return account.refund(amount);
    }

    // --- Administrative Controls ---

    @Override
    public void blacklistTag(String tagId, String reason) {
        if (tagId == null) return;
        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(formattedTag);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException(formattedTag);
        }
        accountOpt.get().blacklist(reason);
    }

    @Override
    public void unblacklistTag(String tagId) {
        if (tagId == null) return;
        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(formattedTag);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException(formattedTag);
        }
        accountOpt.get().unblacklist();
    }

    @Override
    public void closeAccount(String tagId) {
        if (tagId == null) return;
        String formattedTag = tagId.trim().toUpperCase();
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(formattedTag);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException(formattedTag);
        }
        accountOpt.get().close();
    }

    @Override
    public boolean isTagValidAndActive(String tagId) {
        if (tagId == null) return false;
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(tagId.trim().toUpperCase());
        if (accountOpt.isEmpty()) return false;
        FASTagAccount acc = accountOpt.get();
        return !acc.isBlacklisted() && acc.getStatus() == AccountStatus.ACTIVE;
    }

    @Override
    public boolean isTagEligibleForToll(String tagId, double tollAmount) {
        if (tagId == null) return false;
        Optional<FASTagAccount> accountOpt = accountRepository.findByTagId(tagId.trim().toUpperCase());
        if (accountOpt.isEmpty()) return false;
        return accountOpt.get().isEligibleForToll(tollAmount);
    }
}
