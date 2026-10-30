package com.toll.module2.account.exception;

public class DuplicateTagException extends FASTagAccountException {
    public DuplicateTagException(String tagId) {
        super("FASTag with Tag ID '" + tagId + "' already exists in the system.");
    }
}
