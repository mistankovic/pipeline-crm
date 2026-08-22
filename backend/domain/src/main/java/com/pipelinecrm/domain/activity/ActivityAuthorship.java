package com.pipelinecrm.domain.activity;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.Guard;

import java.time.Instant;

/** Who recorded an activity and when it happened. */
public record ActivityAuthorship(UserId author, Instant occurredAt) {

    public ActivityAuthorship {
        Guard.present(author, "author of an activity");
        Guard.present(occurredAt, "time an activity occurred");
    }
}
