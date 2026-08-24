package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.DomainException;

public final class DealStageNotAuthorizedException extends DomainException {

    public DealStageNotAuthorizedException() {
        super("only the deal owner or a manager may change deal stage");
    }
}
