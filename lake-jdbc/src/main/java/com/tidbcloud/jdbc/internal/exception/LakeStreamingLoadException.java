package com.tidbcloud.jdbc.internal.exception;

public class LakeStreamingLoadException extends LakeOperationException {
    public LakeStreamingLoadException(String message) {
        super(message);
    }

    public LakeStreamingLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
