package com.pipelinecrm.domain.deal;

import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Guard;

/**
 * A deal's whole state as plain data. Persistence adapters read and write deals through
 * this type, so that no mapper needs access to the entity's internals and the entity
 * needs no getters it would not otherwise have.
 */
public record DealSnapshot(DealId id, String title, DealParties parties, DealTerms terms, DealStage stage) {

    public DealSnapshot {
        Guard.present(id, "deal id");
        title = Guard.filled(title, "deal title");
        Guard.present(parties, "parties to a deal");
        Guard.present(terms, "terms of a deal");
        Guard.present(stage, "stage of a deal");
    }
}
