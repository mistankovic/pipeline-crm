package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;

public interface CreateDealUseCase {

    DealId execute(Command command);

    record Command(UserId actorId, CompanyId companyId, UserId ownerId, String title, Money value, Probability probability) {}
}
