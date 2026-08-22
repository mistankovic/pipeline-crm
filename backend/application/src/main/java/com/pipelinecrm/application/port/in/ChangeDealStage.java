package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.DealView;

import java.util.UUID;

/**
 * Move a deal along the pipeline. Every rule about whether the move is allowed lives
 * below this line, in the domain; this port only carries the request.
 */
public interface ChangeDealStage {

    DealView handle(StageChange change);

    record StageChange(UUID dealId, String targetStage, UUID actorId) {
    }
}
