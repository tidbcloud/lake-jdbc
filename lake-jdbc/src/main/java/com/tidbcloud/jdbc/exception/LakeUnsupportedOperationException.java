package com.tidbcloud.jdbc.exception;

public class LakeUnsupportedOperationException extends UnsupportedOperationException {

    public static final String OPERATION_NOT_SUPPORTED = "JDBC Operation not supported. Method: %s, Line: %d";

    public LakeUnsupportedOperationException() {
        super(String.format(OPERATION_NOT_SUPPORTED, Thread.currentThread().getStackTrace()[2].getMethodName(),
                Thread.currentThread().getStackTrace()[2].getLineNumber()));
    }
}
