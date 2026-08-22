package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ViewDeal;
import com.pipelinecrm.application.view.DealViews;
import com.pipelinecrm.domain.deal.Deal;

import java.util.UUID;

/** One deal and everything recorded against it. */
public final class ViewDealInteractor implements ViewDeal {

    private final DealHistory history;
    private final Parties parties;
    private final Timelines timelines;

    public ViewDealInteractor(DealHistory history, Parties parties, Timelines timelines) {
        this.history = history;
        this.parties = parties;
        this.timelines = timelines;
    }

    @Override
    public DealDetail handle(UUID dealId, UUID callerId) {
        Deal deal = history.deal(dealId);
        return new DealDetail(
                DealViews.asSeenBy(deal, parties.companyOf(deal), parties.ownerOf(deal),
                        parties.user(callerId)),
                timelines.of(history.of(deal).activities()));
    }
}
