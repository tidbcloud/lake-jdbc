package com.tidbcloud.jdbc.internal.exception;

public class LakeOperationException extends RuntimeException {
    public LakeOperationException(String message) {
        super(message);
    }

    public LakeOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
