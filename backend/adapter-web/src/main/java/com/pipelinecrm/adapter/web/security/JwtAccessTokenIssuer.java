package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.application.port.out.AccessTokenIssuer;
import com.pipelinecrm.domain.user.User;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/** Turns a signed-in user into a signed JWT. The application layer only sees the port. */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtSettings settings;
    private final SecretKey key;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtSettings settings, Clock clock) {
        this.settings = settings;
        this.key = JwtKeys.from(settings);
        this.clock = clock;
    }

    @Override
    public IssuedToken issueFor(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(settings.validity());
        String token = Jwts.builder()
                .issuer(settings.issuer())
                .subject(user.id().value().toString())
                .claim("role", user.role().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }
}
