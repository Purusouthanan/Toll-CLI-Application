package com.toll.module2.account.exception;

public class AccountBlockedException extends FASTagAccountException {
    public AccountBlockedException(String tagId, double balance) {
        super(String.format("FASTag '%s' is BLOCKED. Current balance Rs. %.2f is below minimum operational threshold of Rs. 100.00. Recharge to restore status.",
                tagId, balance));
    }
}
