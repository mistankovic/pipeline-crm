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

    /**
     * A deal as one particular caller sees it.
     *
     * <p>There is deliberately no overload that omits the caller. A default of "the owner" was
     * written first and was exactly the defect being fixed: every reader would have been told
     * the owner's permissions. Making the argument mandatory forced every read to say who is
     * asking, which is what they should always have done.
     */
    public static DealView asSeenBy(Deal deal, Company company, User owner, User caller) {
        return new DealView(
                deal.id().value(),
                deal.title(),
                CompanyViews.of(company),
                UserViews.of(owner),
                MoneyViews.of(deal.value()),
                deal.probability().percentage(),
                deal.stage().name(),
                MoneyViews.of(deal.weightedValue()),
                deal.transitionsAllowedFor(caller).stream().map(DealStage::name).toList(),
                deal.mayBeChangedBy(caller));
    }
}
