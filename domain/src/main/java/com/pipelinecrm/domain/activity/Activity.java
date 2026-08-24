package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guards;
import java.time.Instant;

public final class Activity {

    private final ActivityId id;
    private final ActivityType type;
    private final ActivityBody body;
    private final ActivityTarget target;
    private final UserId createdBy;
    private final Instant createdAt;

    private Activity(
            ActivityId id,
            ActivityType type,
            ActivityBody body,
            ActivityTarget target,
            UserId createdBy,
            Instant createdAt) {
        this.id = Guards.notNull(id, "id");
        this.type = Guards.notNull(type, "type");
        this.body = Guards.notNull(body, "body");
        this.target = Guards.notNull(target, "target");
        this.createdBy = Guards.notNull(createdBy, "createdBy");
        this.createdAt = Guards.notNull(createdAt, "createdAt");
    }

    public static Activity record(
            ActivityId id,
            ActivityType type,
            ActivityBody body,
            ActivityTarget target,
            UserId createdBy,
            Instant createdAt) {
        return new Activity(id, type, body, target, createdBy, createdAt);
    }

    public ActivityId id() {
        return id;
    }

    public ActivityType type() {
        return type;
    }

    public ActivityBody body() {
        return body;
    }

    public ActivityTarget target() {
        return target;
    }

    public UserId createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean qualifiesDealWin(DealId dealId) {
        return type.qualifiesCloseWon() && target.isDeal(dealId);
    }
}
