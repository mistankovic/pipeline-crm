package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.activity.DealActivities;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;

import java.util.UUID;

/**
 * A deal together with what has been recorded against it.
 *
 * <p>They travel together because the domain's own rules need both at once: whether a deal
 * may be won depends on its history, and the history has to be the history of *that* deal.
 */
public final class DealHistory {

    private final DealRepository deals;
    private final ActivityRepository activities;

    public DealHistory(DealRepository deals, ActivityRepository activities) {
        this.deals = deals;
        this.activities = activities;
    }

    public Deal deal(UUID id) {
        DealId dealId = DealId.of(id);
        return Required.found(deals.findById(dealId), "deal", dealId);
    }

    public DealActivities of(Deal deal) {
        return activities.findByDeal(deal.id());
    }

    public void save(Deal deal) {
        deals.save(deal);
    }
}
