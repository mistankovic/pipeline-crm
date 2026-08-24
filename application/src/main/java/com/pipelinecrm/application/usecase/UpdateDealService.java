package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealTitle;

public final class UpdateDealService implements UpdateDealUseCase {

    private final DealRepository deals;

    public UpdateDealService(DealRepository deals) {
        this.deals = deals;
    }

    @Override
    public void execute(Command command) {
        Deal deal = deals.findById(command.dealId()).orElseThrow(() -> new NotFoundException("deal"));
        deal.rename(DealTitle.of(command.title()));
        deal.revalue(command.value());
        deal.changeProbability(command.probability());
        deals.save(deal);
    }
}
