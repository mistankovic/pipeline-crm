package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;
import java.util.UUID;

public record ContactId(UUID value) {

    public ContactId {
        Guards.notNull(value, "value");
    }

    public static ContactId generate() {
        return new ContactId(UUID.randomUUID());
    }

    public static ContactId parse(String raw) {
        return new ContactId(UUID.fromString(Guards.notBlank(raw, "raw")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
