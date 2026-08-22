package com.pipelinecrm.application.error;

/**
 * A use case refusing to proceed for a reason that is not a domain rule: something was not
 * found, or the caller is not who they claim to be. Domain rule failures stay in the
 * domain and keep their own exception types.
 */
public abstract class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ApplicationException(String message) {
        super(message);
    }
}
