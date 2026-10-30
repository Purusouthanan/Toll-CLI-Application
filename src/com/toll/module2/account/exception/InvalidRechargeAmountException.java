package com.toll.module2.account.exception;

public class InvalidRechargeAmountException extends FASTagAccountException {
    public InvalidRechargeAmountException(double amount) {
        super(String.format("Invalid recharge amount: Rs. %.2f. A recharge must be at least Rs. 100.00 and at most Rs. 10,000.00.", amount));
    }
}
