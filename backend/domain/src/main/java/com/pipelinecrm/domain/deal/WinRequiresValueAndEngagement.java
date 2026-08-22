package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.shared.DomainException;

/** Rule: a deal is only won if it is worth something and somebody actually spoke to the customer. */
public final class WinRequiresValueAndEngagement extends DomainException {

    private static final long serialVersionUID = 1L;

    public WinRequiresValueAndEngagement(String reason) {
        super("this deal cannot be won: " + reason);
    }
}
