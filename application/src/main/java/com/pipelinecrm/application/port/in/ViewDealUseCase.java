package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.identity.DealId;
import java.util.List;

public interface ViewDealUseCase {

    Result execute(DealId dealId);

    record Result(Deal deal, List<Activity> activities) {}
}
