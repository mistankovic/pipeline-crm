package com.pipelinecrm.application.view;

import java.time.Instant;

/** The result of a successful sign-in. */
public record AuthenticatedUser(UserView user, String token, Instant expiresAt) {
}
