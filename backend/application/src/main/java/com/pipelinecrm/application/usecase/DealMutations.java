package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.out.Transactions;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.application.view.DealViews;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.user.User;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Everything that changing a deal has in common: find it, find who is asking, let the deal
 * decide, write it back, describe the result — all inside one unit of work.
 *
 * <p>The three use cases that change a deal were three copies of this, differing only in the
 * middle line. The duplication gate found them. What is left in each use case is the line
 * that is actually about that use case.
 */
public final class DealMutations {

    private final DealHistory history;
    private final Parties parties;
    private final Transactions transactions;

    public DealMutations(DealHistory history, Parties parties, Transactions transactions) {
        this.history = history;
        this.parties = parties;
        this.transactions = transactions;
    }

    public DealView apply(UUID dealId, UUID actorId, BiConsumer<Deal, User> change) {
        return transactions.execute(() -> mutate(dealId, actorId, change));
    }

    private DealView mutate(UUID dealId, UUID actorId, BiConsumer<Deal, User> change) {
        Deal deal = history.deal(dealId);
        User actor = parties.user(actorId);

        change.accept(deal, actor);

        history.save(deal);
        return DealViews.asSeenBy(deal, parties.companyOf(deal), parties.ownerOf(deal), actor);
    }
}
