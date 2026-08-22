package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.DomainException;

/** Rule: a deal moves one step forward, or is lost, and never leaves a closed stage. */
public final class IllegalStageTransition extends DomainException {

    private static final long serialVersionUID = 1L;

    public IllegalStageTransition(DealStage from, DealStage to) {
        super("a deal in " + from + " cannot move to " + to);
    }
}
