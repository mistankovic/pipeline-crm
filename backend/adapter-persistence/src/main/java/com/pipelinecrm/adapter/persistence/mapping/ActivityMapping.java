package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.row.ActivityRow;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityAuthorship;
import com.pipelinecrm.domain.activity.ActivitySubject;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.activity.ContactSubject;
import com.pipelinecrm.domain.activity.DealSubject;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.InvariantViolation;

/**
 * Activity rows in, activities out, and back again.
 *
 * <p>The sealed subject becomes two nullable columns on the way out and is reconstructed on
 * the way in. A row with neither column set — which the check constraint forbids, but which a
 * hand-edited database could still contain — is rejected here rather than turned into a
 * half-built domain object.
 */
public final class ActivityMapping {

    private ActivityMapping() {
    }

    public static Activity toDomain(ActivityRow row) {
        return new Activity(ActivityId.of(row.getId()), subjectOf(row),
                ActivityType.valueOf(row.getType()), row.getSummary(),
                new ActivityAuthorship(UserId.of(row.getAuthorId()), row.getOccurredAt()));
    }

    public static ActivityRow toRow(Activity activity) {
        return new ActivityRow(activity.id().value(), columnsFor(activity.subject()),
                new ActivityRow.ActivityContent(activity.type().name(), activity.summary()),
                new ActivityRow.ActivityRecord(activity.authorship().author().value(),
                        activity.authorship().occurredAt()));
    }

    private static ActivitySubject subjectOf(ActivityRow row) {
        if (row.getDealId() != null) {
            return new DealSubject(DealId.of(row.getDealId()));
        }
        if (row.getContactId() != null) {
            return new ContactSubject(ContactId.of(row.getContactId()));
        }
        throw new InvariantViolation("activity " + row.getId() + " is about neither a deal nor a contact");
    }

    private static ActivityRow.ActivitySubjectColumns columnsFor(ActivitySubject subject) {
        return switch (subject) {
            case DealSubject onDeal -> new ActivityRow.ActivitySubjectColumns(onDeal.deal().value(), null);
            case ContactSubject onContact ->
                    new ActivityRow.ActivitySubjectColumns(null, onContact.contact().value());
        };
    }
}
