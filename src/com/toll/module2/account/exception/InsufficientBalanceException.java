package com.toll.module2.account.exception;

public class InsufficientBalanceException extends FASTagAccountException {
    private final double currentBalance;
    private final double requiredAmount;

    public InsufficientBalanceException(String tagId, double currentBalance, double requiredAmount) {
        super(String.format("FASTag '%s' has insufficient balance. Current: Rs. %.2f, Required: Rs. %.2f. Balance cannot be negative.",
                tagId, currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }
}
