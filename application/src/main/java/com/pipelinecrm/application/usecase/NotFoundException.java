package com.pipelinecrm.application.usecase;

public final class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
