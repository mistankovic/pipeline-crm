package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.CreateDealUseCase;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealTitle;
import com.pipelinecrm.domain.identity.DealId;

public final class CreateDealService implements CreateDealUseCase {

    private final DealRepository deals;
    private final CompanyRepository companies;
    private final UserRepository users;

    public CreateDealService(DealRepository deals, CompanyRepository companies, UserRepository users) {
        this.deals = deals;
        this.companies = companies;
        this.users = users;
    }

    @Override
    public DealId execute(Command command) {
        companies.findById(command.companyId()).orElseThrow(() -> new NotFoundException("company"));
        users.findById(command.ownerId()).orElseThrow(() -> new NotFoundException("owner"));
        DealId id = DealId.generate();
        Deal deal = Deal.open(
                id, command.companyId(), command.ownerId(), DealTitle.of(command.title()), command.value(), command.probability());
        deals.save(deal);
        return id;
    }
}
