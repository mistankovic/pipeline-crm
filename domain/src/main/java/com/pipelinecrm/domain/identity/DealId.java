package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;
import java.util.UUID;

public record DealId(UUID value) {

    public DealId {
        Guards.notNull(value, "value");
    }

    public static DealId generate() {
        return new DealId(UUID.randomUUID());
    }

    public static DealId parse(String raw) {
        return new DealId(UUID.fromString(Guards.notBlank(raw, "raw")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
