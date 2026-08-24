package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;

public interface UpdateDealUseCase {

    void execute(Command command);

    record Command(UserId actorId, DealId dealId, String title, Money value, Probability probability) {}
}
