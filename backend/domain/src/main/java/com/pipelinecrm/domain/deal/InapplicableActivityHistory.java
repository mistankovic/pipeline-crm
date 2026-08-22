package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.DomainException;

/**
 * Raised when a deal is asked to judge itself against another deal's activity history.
 * This is a programming error in an outer layer, caught by the domain rather than trusted.
 */
public final class InapplicableActivityHistory extends DomainException {

    private static final long serialVersionUID = 1L;

    public InapplicableActivityHistory(DealId deal) {
        super("the activity history supplied does not belong to deal " + deal.value());
    }
}
