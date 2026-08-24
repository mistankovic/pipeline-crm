package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ViewDealUseCase;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;

public final class ViewDealService implements ViewDealUseCase {

    private final DealRepository deals;

    public ViewDealService(DealRepository deals) {
        this.deals = deals;
    }

    @Override
    public Result execute(DealId dealId) {
        Deal deal = deals.findById(dealId).orElseThrow(() -> new NotFoundException("deal"));
        return new Result(deal, deal.activities());
    }
}
