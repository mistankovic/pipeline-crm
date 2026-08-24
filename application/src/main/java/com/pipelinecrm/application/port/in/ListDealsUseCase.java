package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.UserId;
import java.util.List;

public interface ListDealsUseCase {

    List<Deal> execute(Filter filter);

    record Filter(DealStage stage, UserId ownerId) {}
}
