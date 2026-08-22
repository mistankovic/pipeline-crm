package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.math.BigDecimal;
import java.util.UUID;

/** Open a new opportunity. It starts as a lead; the caller does not get to choose. */
public interface CreateDeal {

    DealView handle(NewDeal deal);

    record NewDeal(
            String title,
            UUID companyId,
            UUID ownerId,
            UUID creatorId,
            BigDecimal value,
            String currency,
            int probability) {
    }
}
