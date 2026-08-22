package com.pipelinecrm.application.port.out;

import com.pipelinecrm.domain.user.User;

import java.time.Instant;

/** Issues the token a signed-in browser presents on later calls. */
public interface AccessTokenIssuer {

    IssuedToken issueFor(User user);

    /** An opaque token and the moment it stops being accepted. */
    record IssuedToken(String value, Instant expiresAt) {
    }
}
