package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.domain.identity.Identifier;

import java.util.Optional;

/**
 * "Look it up, and if it is not there say which thing was missing." Every use case needs
 * this and none of them should write it again.
 */
final class Required {

    private Required() {
    }

    static <T> T found(Optional<T> candidate, Identifier id) {
        return candidate.orElseThrow(() -> new UnknownEntity(id));
    }
}
