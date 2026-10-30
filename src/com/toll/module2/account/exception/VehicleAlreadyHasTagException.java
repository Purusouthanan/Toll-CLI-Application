package com.toll.module2.account.exception;

public class VehicleAlreadyHasTagException extends FASTagAccountException {
    public VehicleAlreadyHasTagException(String regNo, String existingTagId) {
        super("Vehicle '" + regNo + "' is already linked to active FASTag '" + existingTagId + 
              "'. A vehicle can have only one active tag.");
    }
}
