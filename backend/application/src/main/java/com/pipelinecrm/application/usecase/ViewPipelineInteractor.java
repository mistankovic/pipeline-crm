package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ViewPipeline;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.application.view.DealViews;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.UserId;

import java.util.List;
import java.util.UUID;

/**
 * The deals on the board. The companies and owners those deals refer to are read in two
 * queries, whatever the size of the board, and a missing one is reported as a missing thing
 * rather than as a null.
 */
public final class ViewPipelineInteractor implements ViewPipeline {

    private final DealRepository deals;
    private final Parties parties;

    public ViewPipelineInteractor(DealRepository deals, Parties parties) {
        this.deals = deals;
        this.parties = parties;
    }

    @Override
    public List<DealView> everything() {
        return viewsOf(deals.findAll());
    }

    @Override
    public List<DealView> ownedBy(UUID ownerId) {
        return viewsOf(deals.findOwnedBy(UserId.of(ownerId)));
    }

    private List<DealView> viewsOf(List<Deal> found) {
        PartyIndex index = parties.indexFor(found);
        return found.stream()
                .map(deal -> DealViews.of(deal, index.companyOf(deal), index.ownerOf(deal)))
                .toList();
    }
}
