package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.application.port.out.TokenIssuer;
import com.pipelinecrm.domain.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtSettings settings;

    public JwtTokenIssuer(JwtSettings settings) {
        this.settings = settings;
    }

    @Override
    public String issue(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(settings.ttlSeconds());
        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email().value())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(settings.secret().getBytes(StandardCharsets.UTF_8));
    }
}
