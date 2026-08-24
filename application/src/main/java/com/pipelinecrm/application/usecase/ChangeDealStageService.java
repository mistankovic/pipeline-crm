package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.user.User;

public final class ChangeDealStageService implements ChangeDealStageUseCase {

    private final DealRepository deals;
    private final UserRepository users;

    public ChangeDealStageService(DealRepository deals, UserRepository users) {
        this.deals = deals;
        this.users = users;
    }

    @Override
    public void execute(Command command) {
        User actor = users.findById(command.actorId()).orElseThrow(() -> new NotFoundException("user"));
        Deal deal = deals.findById(command.dealId()).orElseThrow(() -> new NotFoundException("deal"));
        deal.changeStage(actor, command.target());
        deals.save(deal);
    }
}
