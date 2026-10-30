package com.toll.module2.account.service;

/**
 * Result DTO encapsulating the outcome of a toll debit operation.
 * Module 3 (Toll Processing) and Module 5 (Billing/Alerts) consume this.
 */
public class DebitResult {
    private final boolean successful;
    private final String tagId;
    private final double debitedAmount;
    private final double remainingBalance;
    private final boolean autoBlocked;
    private final boolean lowBalanceAlertTriggered;
    private final String message;

    public DebitResult(boolean successful, String tagId, double debitedAmount, double remainingBalance,
                       boolean autoBlocked, boolean lowBalanceAlertTriggered, String message) {
        this.successful = successful;
        this.tagId = tagId;
        this.debitedAmount = debitedAmount;
        this.remainingBalance = remainingBalance;
        this.autoBlocked = autoBlocked;
        this.lowBalanceAlertTriggered = lowBalanceAlertTriggered;
        this.message = message;
    }

    public static DebitResult success(String tagId, double debitedAmount, double remainingBalance,
                                      boolean autoBlocked, boolean lowBalanceAlert) {
        String msg = String.format("Debited Rs. %.2f successfully. Remaining Balance: Rs. %.2f%s%s",
                debitedAmount, remainingBalance,
                autoBlocked ? " [AUTO-BLOCKED: Balance < Rs. 100]" : "",
                lowBalanceAlert ? " [LOW BALANCE ALERT: Balance < Rs. 200]" : "");
        return new DebitResult(true, tagId, debitedAmount, remainingBalance, autoBlocked, lowBalanceAlert, msg);
    }

    public static DebitResult failure(String tagId, double attemptedAmount, double currentBalance, String errorReason) {
        return new DebitResult(false, tagId, attemptedAmount, currentBalance, false, false, errorReason);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getTagId() {
        return tagId;
    }

    public double getDebitedAmount() {
        return debitedAmount;
    }

    public double getRemainingBalance() {
        return remainingBalance;
    }

    public boolean isAutoBlocked() {
        return autoBlocked;
    }

    public boolean isLowBalanceAlertTriggered() {
        return lowBalanceAlertTriggered;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
