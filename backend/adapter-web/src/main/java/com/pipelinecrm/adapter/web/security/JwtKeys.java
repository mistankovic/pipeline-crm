package com.pipelinecrm.adapter.web.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/** Derives the signing key from the configured secret, in one place. */
final class JwtKeys {

    private JwtKeys() {
    }

    static SecretKey from(JwtSettings settings) {
        return Keys.hmacShaKeyFor(settings.secret().getBytes(StandardCharsets.UTF_8));
    }
}
