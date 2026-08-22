package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guard;
import java.util.UUID;

/** Identity of a user. */
public record UserId(UUID value) implements Identifier {

    public UserId {
        Guard.present(value, "user id");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(Guard.filled(value, "user id")));
    }
}
