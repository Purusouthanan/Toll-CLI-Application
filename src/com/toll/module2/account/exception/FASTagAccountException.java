package com.toll.module2.account.exception;

public class FASTagAccountException extends RuntimeException {
    public FASTagAccountException(String message) {
        super(message);
    }

    public FASTagAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
