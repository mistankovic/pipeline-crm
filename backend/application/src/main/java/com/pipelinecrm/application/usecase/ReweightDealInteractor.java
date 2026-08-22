package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ReweightDeal;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.shared.Probability;

/** Changes how likely an open deal is. Whether this user may do so is the deal's decision. */
public final class ReweightDealInteractor implements ReweightDeal {

    private final DealMutations mutations;

    public ReweightDealInteractor(DealMutations mutations) {
        this.mutations = mutations;
    }

    @Override
    public DealView handle(Reweighting reweighting) {
        return mutations.apply(reweighting.dealId(), reweighting.actorId(),
                (deal, actor) -> deal.reweight(Probability.of(reweighting.probability()), actor));
    }
}
