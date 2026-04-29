package com.tidbcloud.jdbc.internal.exception;

public class LakePresignException extends LakeOperationException {
    public LakePresignException(String message) {
        super(message);
    }

    public LakePresignException(String message, Throwable cause) {
        super(message, cause);
    }
}
