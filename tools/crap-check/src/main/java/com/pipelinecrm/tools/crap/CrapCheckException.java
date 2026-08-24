package com.pipelinecrm.tools.crap;

public final class CrapCheckException extends RuntimeException {

    public CrapCheckException(String message) {
        super(message);
    }

    public CrapCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
