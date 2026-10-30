package com.toll.module2.account.model;

import com.toll.common.AccountStatus;
import com.toll.module2.account.exception.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a FASTag Account.
 * Demonstrates:
 * - Strict Encapsulation: All state fields are private and mutable only via validated business methods.
 * - Automatic State Machine:
 *     * Auto-Block when balance < Rs. 100.00
 *     * Auto-Unblock when balance becomes >= Rs. 100.00 after a recharge
 * - Indian NHAI FASTag Business Rules:
 *     * Minimum recharge: Rs. 100.00
 *     * Maximum recharge: Rs. 10,000.00
 *     * Maximum account balance cap: Rs. 50,000.00
 *     * Non-negative balance constraint
 *     * Admin blacklisting with plaza refusal
 */
public class FASTagAccount {
    public static final double MIN_OPERATIONAL_BALANCE = 100.00;
    public static final double MAX_ACCOUNT_BALANCE = 50000.00;
    public static final double MIN_RECHARGE_AMOUNT = 100.00;
    public static final double MAX_RECHARGE_AMOUNT = 10000.00;
    public static final double LOW_BALANCE_ALERT_THRESHOLD = 200.00;

    private final String tagId;
    private final String vehicleRegistrationNumber;
    private double balance;
    private AccountStatus status;
    private boolean blacklisted;
    private String blacklistReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor for issuing a new FASTag account linked to a vehicle.
     *
     * @param tagId                     Unique Tag Identifier (e.g. IND-TAG-1001)
     * @param vehicleRegistrationNumber Unique Vehicle registration number (1:1 link)
     * @param initialBalance            Initial deposit (must be >= 100 and <= 50000)
     */
    public FASTagAccount(String tagId, String vehicleRegistrationNumber, double initialBalance) {
        if (tagId == null || tagId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag ID cannot be null or empty.");
        }
        if (vehicleRegistrationNumber == null || vehicleRegistrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle registration number cannot be null or empty.");
        }
        if (initialBalance < 0.0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        if (initialBalance > MAX_ACCOUNT_BALANCE) {
            throw new BalanceLimitExceededException(0.0, initialBalance, MAX_ACCOUNT_BALANCE);
        }

        this.tagId = tagId.trim().toUpperCase();
        this.vehicleRegistrationNumber = vehicleRegistrationNumber.trim().toUpperCase();
        this.balance = initialBalance;
        this.status = (initialBalance < MIN_OPERATIONAL_BALANCE) ? AccountStatus.BLOCKED : AccountStatus.ACTIVE;
        this.blacklisted = false;
        this.blacklistReason = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Encapsulated Getters ---

    public String getTagId() {
        return tagId;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized AccountStatus getStatus() {
        return status;
    }

    public synchronized boolean isBlacklisted() {
        return blacklisted;
    }

    public synchronized String getBlacklistReason() {
        return blacklistReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public synchronized LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // --- Domain Business Logic & State Mutations ---

    /**
     * Executes a recharge on the FASTag account.
     * Rule: Must be >= Rs. 100, <= Rs. 10,000, and cannot cause balance to exceed Rs. 50,000.
     * Rule: If the account was BLOCKED, it automatically becomes ACTIVE if new balance >= Rs. 100.
     *
     * @param amount The recharge amount
     * @return true if the account was automatically unblocked (transitioned from BLOCKED -> ACTIVE)
     */
    public synchronized boolean recharge(double amount) {
        if (status == AccountStatus.CLOSED) {
            throw new FASTagAccountException("Cannot recharge a CLOSED FASTag account: " + tagId);
        }
        if (amount < MIN_RECHARGE_AMOUNT || amount > MAX_RECHARGE_AMOUNT) {
            throw new InvalidRechargeAmountException(amount);
        }
        if (balance + amount > MAX_ACCOUNT_BALANCE) {
            throw new BalanceLimitExceededException(balance, amount, MAX_ACCOUNT_BALANCE);
        }

        AccountStatus previousStatus = this.status;
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();

        boolean autoUnblocked = false;
        // Auto-unblock rule: If previously BLOCKED and new balance >= 100, restore to ACTIVE
        if (previousStatus == AccountStatus.BLOCKED && this.balance >= MIN_OPERATIONAL_BALANCE) {
            this.status = AccountStatus.ACTIVE;
            autoUnblocked = true;
        }

        return autoUnblocked;
    }

    /**
     * Debits toll amount from the FASTag balance.
     * Rule: Balance can never be negative.
     * Rule: Refuse if blacklisted, closed, or blocked.
     * Rule: If balance drops below Rs. 100 after deduction, automatically transition to BLOCKED.
     *
     * @param amount Toll amount to deduct
     * @return true if the account automatically transitioned to BLOCKED (< Rs. 100)
     */
    public synchronized boolean debit(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Debit amount cannot be negative: " + amount);
        }
        if (blacklisted) {
            throw new TagBlacklistedException(tagId, blacklistReason);
        }
        if (status == AccountStatus.CLOSED) {
            throw new FASTagAccountException("FASTag account '" + tagId + "' is CLOSED.");
        }
        if (status == AccountStatus.BLOCKED) {
            throw new AccountBlockedException(tagId, balance);
        }
        if (balance - amount < 0.0) {
            throw new InsufficientBalanceException(tagId, balance, amount);
        }

        this.balance -= amount;
        this.updatedAt = LocalDateTime.now();

        boolean autoBlocked = false;
        // Auto-block rule: If balance drops below Rs. 100, status becomes BLOCKED
        if (this.balance < MIN_OPERATIONAL_BALANCE) {
            this.status = AccountStatus.BLOCKED;
            autoBlocked = true;
        }

        return autoBlocked;
    }

    /**
     * Credits a refund back into the FASTag account (e.g. from an approved dispute in Module 5).
     *
     * @param amount Amount to refund
     * @return true if account was unblocked from BLOCKED to ACTIVE
     */
    public synchronized boolean refund(double amount) {
        if (amount <= 0.0) {
            throw new IllegalArgumentException("Refund amount must be positive: " + amount);
        }
        if (status == AccountStatus.CLOSED) {
            throw new FASTagAccountException("Cannot refund to a CLOSED account: " + tagId);
        }
        if (balance + amount > MAX_ACCOUNT_BALANCE) {
            throw new BalanceLimitExceededException(balance, amount, MAX_ACCOUNT_BALANCE);
        }

        AccountStatus previousStatus = this.status;
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();

        boolean autoUnblocked = false;
        if (previousStatus == AccountStatus.BLOCKED && this.balance >= MIN_OPERATIONAL_BALANCE) {
            this.status = AccountStatus.ACTIVE;
            autoUnblocked = true;
        }
        return autoUnblocked;
    }

    /**
     * Admin operation: Blacklists a stolen, fraudulent, or suspended tag.
     * Blacklisted tags are refused at every plaza.
     */
    public synchronized void blacklist(String reason) {
        this.blacklisted = true;
        this.blacklistReason = (reason == null || reason.trim().isEmpty()) ? "Stolen or fraudulent tag flag" : reason.trim();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Admin operation: Unblacklists a previously blacklisted tag.
     */
    public synchronized void unblacklist() {
        this.blacklisted = false;
        this.blacklistReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Closes the FASTag account permanently.
     */
    public synchronized void close() {
        this.status = AccountStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if the tag is eligible for immediate automated toll deduction.
     */
    public synchronized boolean isEligibleForToll(double tollAmount) {
        return !blacklisted && status == AccountStatus.ACTIVE && balance >= tollAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FASTagAccount that = (FASTagAccount) o;
        return Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

    @Override
    public synchronized String toString() {
        return String.format("FASTag[%s | Vehicle: %s | Bal: Rs. %.2f | Status: %s | Blacklisted: %s]",
                tagId, vehicleRegistrationNumber, balance, status, blacklisted ? "YES (" + blacklistReason + ")" : "NO");
    }
}
