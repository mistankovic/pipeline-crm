package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guards;
import java.util.UUID;

public record CompanyId(UUID value) {

    public CompanyId {
        Guards.notNull(value, "value");
    }

    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID());
    }

    public static CompanyId parse(String raw) {
        return new CompanyId(UUID.fromString(Guards.notBlank(raw, "raw")));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
