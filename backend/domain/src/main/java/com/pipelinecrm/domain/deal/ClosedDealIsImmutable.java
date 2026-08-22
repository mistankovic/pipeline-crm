package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.DomainException;

/** Rule: once a deal is won or lost its value and probability are history, not working data. */
public final class ClosedDealIsImmutable extends DomainException {

    private static final long serialVersionUID = 1L;

    public ClosedDealIsImmutable(DealStage stage) {
        super("a deal in " + stage + " can no longer be changed");
    }
}
