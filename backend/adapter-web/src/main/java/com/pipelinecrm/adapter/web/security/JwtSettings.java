package com.pipelinecrm.adapter.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * How tokens are signed and how long they last.
 *
 * <p>Demo grade, as CONSTITUTION.md section 6 says out loud: one symmetric secret, no
 * rotation, no refresh, no revocation list. The secret must be supplied by the
 * environment; the application refuses to start with a placeholder.
 */
@ConfigurationProperties(prefix = "pipelinecrm.security.jwt")
public record JwtSettings(String secret, Duration validity, String issuer) {

    private static final int SHORTEST_USABLE_SECRET = 32;
    private static final Duration DEFAULT_VALIDITY = Duration.ofHours(8);

    public JwtSettings {
        if (secret == null || secret.length() < SHORTEST_USABLE_SECRET) {
            throw new IllegalStateException(
                    "pipelinecrm.security.jwt.secret must be set to at least "
                            + SHORTEST_USABLE_SECRET + " characters");
        }
        validity = validity == null ? DEFAULT_VALIDITY : validity;
        issuer = issuer == null ? "pipelinecrm" : issuer;
    }
}
