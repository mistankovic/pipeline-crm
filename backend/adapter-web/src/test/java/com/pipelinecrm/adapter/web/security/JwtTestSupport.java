package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.EmailAddress;
import com.pipelinecrm.domain.user.User;
import com.pipelinecrm.domain.user.UserRole;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

/** Shared fixtures for the two security tests, so neither has to hand-roll a token. */
final class JwtTestSupport {

    static final String SECRET = "a-test-secret-that-is-certainly-long-enough";
    static final String OTHER_SECRET = "a-different-secret-that-is-also-long-enough";
    static final String ISSUER = "pipelinecrm-test";
    static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    private JwtTestSupport() {
    }

    static JwtSettings settings() {
        return new JwtSettings(SECRET, Duration.ofHours(8), ISSUER);
    }

    static Clock frozenClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    static User someUser(UserId id) {
        return new User(id, EmailAddress.of("sam@example.com"), "Sam", UserRole.SALES);
    }

    static UserId someUserId() {
        return UserId.of(UUID.randomUUID());
    }

    static String tokenSignedWith(String secret, String issuer, String subject, Instant expiry) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("role", "SALES")
                .issuedAt(Date.from(NOW.minus(Duration.ofMinutes(1))))
                .expiration(Date.from(expiry))
                .signWith(key(secret))
                .compact();
    }

    private static SecretKey key(String secret) {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
