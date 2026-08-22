package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.shared.Guard;

/** A single recorded interaction: a note, a call or a meeting. */
public record Activity(
        ActivityId id,
        ActivitySubject subject,
        ActivityType type,
        String summary,
        ActivityAuthorship authorship) {

    public Activity {
        Guard.present(id, "activity id");
        Guard.present(subject, "subject of an activity");
        Guard.present(type, "activity type");
        summary = Guard.filled(summary, "activity summary");
        Guard.present(authorship, "authorship of an activity");
    }

    public boolean isEngagement() {
        return type.isEngagement();
    }

    public boolean isAbout(DealId deal) {
        return subject instanceof DealSubject linked && linked.deal().equals(deal);
    }

    public boolean isAbout(ContactId contact) {
        return subject instanceof ContactSubject linked && linked.contact().equals(contact);
    }
}
