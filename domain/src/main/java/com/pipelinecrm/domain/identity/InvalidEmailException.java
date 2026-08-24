package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.DomainException;

public final class InvalidEmailException extends DomainException {

    public InvalidEmailException(String value) {
        super("invalid email: " + value);
    }
}
