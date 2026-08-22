package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.RepriceDeal;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.shared.Money;

/** Changes what an open deal is worth. Whether this user may do so is the deal's decision. */
public final class RepriceDealInteractor implements RepriceDeal {

    private final DealMutations mutations;

    public RepriceDealInteractor(DealMutations mutations) {
        this.mutations = mutations;
    }

    @Override
    public DealView handle(Repricing repricing) {
        return mutations.apply(repricing.dealId(), repricing.actorId(),
                (deal, actor) -> deal.reprice(Money.of(repricing.amount(), repricing.currency()), actor));
    }
}
