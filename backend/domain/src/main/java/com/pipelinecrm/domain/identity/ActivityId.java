package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guard;
import java.util.UUID;

/** Identity of a activity. */
public record ActivityId(UUID value) implements Identifier {

    public ActivityId {
        Guard.present(value, "activity id");
    }

    public static ActivityId of(UUID value) {
        return new ActivityId(value);
    }

    public static ActivityId fromString(String value) {
        return new ActivityId(UUID.fromString(Guard.filled(value, "activity id")));
    }
}
