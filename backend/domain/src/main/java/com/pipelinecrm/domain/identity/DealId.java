package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guard;
import java.util.UUID;

/** Identity of a deal. */
public record DealId(UUID value) implements Identifier {

    public DealId {
        Guard.present(value, "deal id");
    }

    public static DealId of(UUID value) {
        return new DealId(value);
    }

    public static DealId fromString(String value) {
        return new DealId(UUID.fromString(Guard.filled(value, "deal id")));
    }
}
