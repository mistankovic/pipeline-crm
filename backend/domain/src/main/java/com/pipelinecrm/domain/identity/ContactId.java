package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.Guard;
import java.util.UUID;

/** Identity of a contact. */
public record ContactId(UUID value) implements Identifier {

    public ContactId {
        Guard.present(value, "contact id");
    }

    public static ContactId of(UUID value) {
        return new ContactId(value);
    }

    public static ContactId fromString(String value) {
        return new ContactId(UUID.fromString(Guard.filled(value, "contact id")));
    }
}
