package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.view.DealView;

/**
 * Moves a deal along the pipeline.
 *
 * <p>Read the body: it hands the deal to itself and gets out of the way. Whether the move is
 * legal, who may make it, and what a close does to the probability are decided by
 * {@code Deal.changeStageTo}. If a rule ever appears in this class, it is in the wrong place.
 */
public final class ChangeDealStageInteractor implements ChangeDealStage {

    private final DealMutations mutations;
    private final DealHistory history;

    public ChangeDealStageInteractor(DealMutations mutations, DealHistory history) {
        this.mutations = mutations;
        this.history = history;
    }

    @Override
    public DealView handle(StageChange change) {
        return mutations.apply(change.dealId(), change.actorId(),
                (deal, actor) -> deal.changeStageTo(change.targetStage(), actor, history.of(deal)));
    }
}
