package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.math.BigDecimal;
import java.util.UUID;

/** Change what an open deal is worth and how likely it is. */
public interface ReviseDeal {

    DealView handle(DealRevision revision);

    record DealRevision(UUID dealId, UUID actorId, BigDecimal value, Integer probability) {
    }
}
