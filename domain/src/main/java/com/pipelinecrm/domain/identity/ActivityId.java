package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;
import java.util.UUID;

public record ActivityId(UUID value) {

    public ActivityId {
        Guards.notNull(value, "value");
    }

    public static ActivityId generate() {
        return new ActivityId(UUID.randomUUID());
    }

    public static ActivityId parse(String raw) {
        return new ActivityId(UUID.fromString(Guards.notBlank(raw, "raw")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
