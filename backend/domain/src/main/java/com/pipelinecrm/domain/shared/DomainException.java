package com.pipelinecrm.domain.shared;

/**
 * Root of every failure the business rules can raise. Outer layers translate subclasses
 * of this into their own vocabulary (HTTP status codes, for instance); the domain itself
 * knows nothing about how the failure will be reported.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }
}
