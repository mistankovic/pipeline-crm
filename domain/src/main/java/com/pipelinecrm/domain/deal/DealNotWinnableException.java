package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.DomainException;

public final class DealNotWinnableException extends DomainException {

    public DealNotWinnableException(String message) {
        super(message);
    }
}
