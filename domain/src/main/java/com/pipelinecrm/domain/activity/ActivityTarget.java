package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Guards;
import java.util.Optional;

public final class ActivityTarget {

    private final DealId dealId;
    private final ContactId contactId;

    private ActivityTarget(DealId dealId, ContactId contactId) {
        this.dealId = dealId;
        this.contactId = contactId;
    }

    public static ActivityTarget deal(DealId dealId) {
        return new ActivityTarget(Guards.notNull(dealId, "dealId"), null);
    }

    public static ActivityTarget contact(ContactId contactId) {
        return new ActivityTarget(null, Guards.notNull(contactId, "contactId"));
    }

    public Optional<DealId> dealId() {
        return Optional.ofNullable(dealId);
    }

    public Optional<ContactId> contactId() {
        return Optional.ofNullable(contactId);
    }

    public boolean isDeal(DealId other) {
        return dealId != null && dealId.equals(other);
    }
}
