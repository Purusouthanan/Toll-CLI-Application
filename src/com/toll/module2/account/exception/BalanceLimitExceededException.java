package com.toll.module2.account.exception;

public class BalanceLimitExceededException extends FASTagAccountException {
    public BalanceLimitExceededException(double currentBalance, double addedAmount, double maxBalance) {
        super(String.format("Recharge rejected: Current balance Rs. %.2f + Rs. %.2f would exceed maximum allowed limit of Rs. %.2f.",
                currentBalance, addedAmount, maxBalance));
    }
}
