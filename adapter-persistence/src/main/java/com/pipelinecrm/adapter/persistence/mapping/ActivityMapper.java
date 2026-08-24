package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.entity.ActivityEntity;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityBody;
import com.pipelinecrm.domain.activity.ActivityTarget;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import java.util.UUID;

public final class ActivityMapper {

    private ActivityMapper() {}

    public static Activity toDomain(ActivityEntity entity) {
        return Activity.record(
                new ActivityId(entity.id()),
                ActivityType.valueOf(entity.type()),
                ActivityBody.of(entity.body()),
                targetOf(entity),
                new UserId(entity.createdBy()),
                entity.createdAt());
    }

    public static ActivityEntity toEntity(Activity activity) {
        UUID dealId = activity.target().dealId().map(DealId::value).orElse(null);
        UUID contactId = activity.target().contactId().map(ContactId::value).orElse(null);
        return new ActivityEntity(
                activity.id().value(),
                activity.type().name(),
                activity.body().value(),
                dealId,
                contactId,
                activity.createdBy().value(),
                activity.createdAt());
    }

    private static ActivityTarget targetOf(ActivityEntity entity) {
        if (entity.dealId() != null) {
            return ActivityTarget.deal(new DealId(entity.dealId()));
        }
        return ActivityTarget.contact(new ContactId(entity.contactId()));
    }
}
