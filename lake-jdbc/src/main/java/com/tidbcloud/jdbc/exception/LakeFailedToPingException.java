package com.tidbcloud.jdbc.exception;


public class LakeFailedToPingException extends RuntimeException {
    public LakeFailedToPingException() {
        super();
    }

    public LakeFailedToPingException(String message) {
        super(message);
    }

    public LakeFailedToPingException(String message, Throwable cause) {
        super(message, cause);
    }

    public LakeFailedToPingException(Throwable cause) {
        super(cause);
    }

    protected LakeFailedToPingException(
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
