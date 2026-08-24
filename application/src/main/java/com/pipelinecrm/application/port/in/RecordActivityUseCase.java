package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;

public interface RecordActivityUseCase {

    ActivityId execute(Command command);

    record Command(UserId actorId, ActivityType type, String body, DealId dealId, ContactId contactId) {}
}
