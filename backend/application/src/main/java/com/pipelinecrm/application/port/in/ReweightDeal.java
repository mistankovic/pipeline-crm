package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.util.UUID;

/** Change how likely an open deal is. */
public interface ReweightDeal {

    DealView handle(Reweighting reweighting);

    record Reweighting(UUID dealId, UUID actorId, int probability) {
    }
}
