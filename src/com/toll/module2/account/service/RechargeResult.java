package com.toll.module2.account.service;

/**
 * Result DTO encapsulating the outcome of a FASTag recharge operation.
 */
public class RechargeResult {
    private final boolean successful;
    private final String tagId;
    private final double rechargeAmount;
    private final double newBalance;
    private final boolean autoUnblocked;
    private final String message;

    public RechargeResult(boolean successful, String tagId, double rechargeAmount, double newBalance,
                          boolean autoUnblocked, String message) {
        this.successful = successful;
        this.tagId = tagId;
        this.rechargeAmount = rechargeAmount;
        this.newBalance = newBalance;
        this.autoUnblocked = autoUnblocked;
        this.message = message;
    }

    public static RechargeResult success(String tagId, double amount, double newBalance, boolean autoUnblocked) {
        String msg = String.format("Recharge of Rs. %.2f successful. New Balance: Rs. %.2f%s",
                amount, newBalance, autoUnblocked ? " [AUTO-UNBLOCKED: Status restored to ACTIVE]" : "");
        return new RechargeResult(true, tagId, amount, newBalance, autoUnblocked, msg);
    }

    public static RechargeResult failure(String tagId, double amount, double currentBalance, String errorReason) {
        return new RechargeResult(false, tagId, amount, currentBalance, false, errorReason);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getTagId() {
        return tagId;
    }

    public double getRechargeAmount() {
        return rechargeAmount;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public boolean isAutoUnblocked() {
        return autoUnblocked;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
