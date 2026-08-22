package com.pipelinecrm.application.testing;

import com.pipelinecrm.application.port.out.AccessTokenIssuer;
import com.pipelinecrm.domain.user.User;

import java.time.Instant;

public final class StubTokens implements AccessTokenIssuer {

    public static final Instant EXPIRY = Instant.parse("2026-03-01T17:00:00Z");

    @Override
    public IssuedToken issueFor(User user) {
        return new IssuedToken("token-for-" + user.id().value(), EXPIRY);
    }
}
