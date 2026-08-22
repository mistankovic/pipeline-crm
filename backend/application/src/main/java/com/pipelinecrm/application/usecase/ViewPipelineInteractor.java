package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ViewPipeline;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.application.view.DealViews;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.user.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The deals on the board. Companies and users are read once and indexed rather than looked
 * up per deal, so a board of fifty deals is three queries and not a hundred and one.
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
        Map<UserId, User> owners = parties.everyone();
        Map<CompanyId, Company> companies = parties.everyCompany();
        return found.stream()
                .map(deal -> DealViews.of(deal, companies.get(deal.parties().company()),
                        owners.get(deal.parties().owner())))
                .toList();
    }
}
