package com.tidbcloud.jdbc.internal.exception;

public class LakeQueryException extends LakeOperationException {
    public LakeQueryException(String message) {
        super(message);
    }

    public LakeQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
