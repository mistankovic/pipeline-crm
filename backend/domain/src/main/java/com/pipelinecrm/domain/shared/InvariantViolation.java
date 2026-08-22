package com.pipelinecrm.domain.shared;

/** Raised when a value object or entity would be constructed in an impossible state. */
public final class InvariantViolation extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvariantViolation(String message) {
        super(message);
    }
}
