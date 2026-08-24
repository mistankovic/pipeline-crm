package com.pipelinecrm.adapter.web;

import com.pipelinecrm.domain.identity.UserId;
import org.springframework.security.core.Authentication;

final class Actors {

    private Actors() {}

    static UserId require(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("unauthenticated");
        }
        return UserId.parse(authentication.getName());
    }
}
