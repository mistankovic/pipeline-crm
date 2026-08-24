package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Guards.notNull(value, "value");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId parse(String raw) {
        return new UserId(UUID.fromString(Guards.notBlank(raw, "raw")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
