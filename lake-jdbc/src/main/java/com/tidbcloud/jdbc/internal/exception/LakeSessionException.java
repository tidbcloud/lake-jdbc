package com.tidbcloud.jdbc.internal.exception;

public class LakeSessionException extends LakeOperationException {
    public LakeSessionException(String message) {
        super(message);
    }

    public LakeSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
