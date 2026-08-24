package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.DomainException;

public final class IllegalDealStageException extends DomainException {

    public IllegalDealStageException(String message) {
        super(message);
    }
}
