package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.RecordActivityUseCase;
import com.pipelinecrm.application.port.out.ActivityRepository;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.activity.ActivityBody;
import com.pipelinecrm.domain.activity.ActivityTarget;
import com.pipelinecrm.domain.identity.ActivityId;
import java.time.Clock;
import java.time.Instant;

public final class RecordActivityService implements RecordActivityUseCase {

    private final ActivityRepository activities;
    private final DealRepository deals;
    private final Clock clock;

    public RecordActivityService(ActivityRepository activities, DealRepository deals, Clock clock) {
        this.activities = activities;
        this.deals = deals;
        this.clock = clock;
    }

    @Override
    public ActivityId execute(Command command) {
        ActivityId id = ActivityId.generate();
        Activity activity = Activity.record(
                id,
                command.type(),
                ActivityBody.of(command.body()),
                targetOf(command),
                command.actorId(),
                Instant.now(clock));
        if (command.dealId() != null) {
            var deal = deals.findById(command.dealId()).orElseThrow(() -> new NotFoundException("deal"));
            deal.recordActivity(activity);
            deals.save(deal);
        }
        activities.save(activity);
        return id;
    }

    private static ActivityTarget targetOf(Command command) {
        if (command.dealId() != null) {
            return ActivityTarget.deal(command.dealId());
        }
        return ActivityTarget.contact(command.contactId());
    }
}
