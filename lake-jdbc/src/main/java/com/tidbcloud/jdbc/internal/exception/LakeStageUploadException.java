package com.tidbcloud.jdbc.internal.exception;

public class LakeStageUploadException extends LakeOperationException {
    public LakeStageUploadException(String message) {
        super(message);
    }

    public LakeStageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
