package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;

public interface ChangeDealStageUseCase {

    void execute(Command command);

    record Command(UserId actorId, DealId dealId, DealStage target) {}
}
