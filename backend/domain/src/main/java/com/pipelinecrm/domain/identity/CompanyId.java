package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guard;
import java.util.UUID;

/** Identity of a company. */
public record CompanyId(UUID value) implements Identifier {

    public CompanyId {
        Guard.present(value, "company id");
    }

    public static CompanyId of(UUID value) {
        return new CompanyId(value);
    }

    public static CompanyId fromString(String value) {
        return new CompanyId(UUID.fromString(Guard.filled(value, "company id")));
    }
}
