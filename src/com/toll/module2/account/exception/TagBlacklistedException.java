package com.toll.module2.account.exception;

public class TagBlacklistedException extends FASTagAccountException {
    public TagBlacklistedException(String tagId, String reason) {
        super(String.format("FASTag '%s' is BLACKLISTED (Reason: %s). Access refused at all toll plazas.", tagId, reason));
    }
}
