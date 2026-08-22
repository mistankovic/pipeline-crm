package com.pipelinecrm.application.view;

import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.user.User;

/**
 * A deal as a caller sees one, including the stages it may legally move to right now. That
 * list is computed by the domain and handed out so the browser knows which columns to offer;
 * the server still refuses an illegal move if a client sends one anyway.
 */
public final class DealViews {

    private DealViews() {
    }

    public static DealView of(Deal deal, Company company, User owner) {
        return new DealView(
                deal.id().value(),
                deal.title(),
                CompanyViews.of(company),
                UserViews.of(owner),
                MoneyViews.of(deal.value()),
                deal.probability().percentage(),
                deal.stage().name(),
                MoneyViews.of(deal.weightedValue()),
                deal.allowedTransitions().stream().map(DealStage::name).toList());
    }
}
