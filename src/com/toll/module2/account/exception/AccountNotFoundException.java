package com.toll.module2.account.exception;

public class AccountNotFoundException extends FASTagAccountException {
    public AccountNotFoundException(String identifier) {
        super("FASTag account not found for identifier: '" + identifier + "'.");
    }
}
