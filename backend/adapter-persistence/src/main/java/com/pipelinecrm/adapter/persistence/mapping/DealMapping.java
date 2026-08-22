package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.row.DealRow;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.deal.DealParties;
import com.pipelinecrm.domain.deal.DealSnapshot;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.DealTerms;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Money;
import com.pipelinecrm.domain.shared.Probability;

/**
 * Deal rows in, deals out, and back again.
 *
 * <p>Both directions go through {@code DealSnapshot}, so the mapper never reaches inside the
 * entity and the entity needs no accessor it would not otherwise have.
 */
public final class DealMapping {

    private DealMapping() {
    }

    public static Deal toDomain(DealRow row) {
        return Deal.from(new DealSnapshot(
                DealId.of(row.getId()),
                row.getTitle(),
                new DealParties(CompanyId.of(row.getCompanyId()), UserId.of(row.getOwnerId())),
                new DealTerms(Money.of(row.getValueAmount(), row.getValueCurrency()),
                        Probability.of(row.getProbability())),
                DealStage.valueOf(row.getStage())));
    }

    public static DealRow toRow(Deal deal) {
        DealSnapshot state = deal.snapshot();
        return new DealRow(state.id().value(), state.title(),
                new DealRow.DealParticipants(state.parties().company().value(),
                        state.parties().owner().value()),
                valuationOf(state));
    }

    private static DealRow.DealValuation valuationOf(DealSnapshot state) {
        return new DealRow.DealValuation(
                state.terms().value().amount(),
                state.terms().value().currency().getCurrencyCode(),
                state.terms().probability().percentage(),
                state.stage().name());
    }
}
