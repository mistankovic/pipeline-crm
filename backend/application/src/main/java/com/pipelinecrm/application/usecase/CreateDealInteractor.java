package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.application.view.DealViews;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealParties;
import com.pipelinecrm.domain.deal.DealTerms;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;
import com.pipelinecrm.domain.user.User;

/**
 * Opens a new opportunity. It always starts as a lead: the caller does not get to say which
 * stage a deal begins in, because that would let anyone skip the pipeline by creating a deal
 * already in negotiation.
 */
public final class CreateDealInteractor implements CreateDeal {

    private final DealRepository deals;
    private final Parties parties;
    private final WritingPorts writing;

    public CreateDealInteractor(DealRepository deals, Parties parties, WritingPorts writing) {
        this.deals = deals;
        this.parties = parties;
        this.writing = writing;
    }

    @Override
    public DealView handle(NewDeal request) {
        return writing.transactions().execute(() -> store(request));
    }

    private DealView store(NewDeal request) {
        Company company = parties.company(request.companyId());
        User owner = parties.user(request.ownerId());
        Deal deal = Deal.open(DealId.of(writing.identifiers().newIdentifier()), request.title(),
                new DealParties(company.id(), owner.id()), termsIn(request));
        deals.save(deal);
        return DealViews.of(deal, company, owner);
    }

    private DealTerms termsIn(NewDeal request) {
        return new DealTerms(Money.of(request.value(), request.currency()),
                Probability.of(request.probability()));
    }
}
