package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.shared.DomainException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Reads the bearer token and puts the caller's identity in the security context. It decides
 * *who* is calling and nothing at all about what they may do.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtSettings settings;
    private final Clock clock;

    public JwtAuthenticationFilter(JwtSettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        bearerTokenOf(request).flatMap(this::identityIn).ifPresent(this::rememberForThisRequest);
        chain.doFilter(request, response);
    }

    private Optional<String> bearerTokenOf(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(PREFIX.length()));
    }

    private Optional<SignedInUser> identityIn(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(JwtKeys.from(settings))
                    .requireIssuer(settings.issuer())
                    // Expiry is judged against the injected clock, not against whatever the
                    // host machine thinks the time is. A filter that cannot be told the time
                    // cannot be tested for expiry.
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new SignedInUser(
                    UserId.fromString(claims.getSubject()), claims.get("role", String.class)));
        } catch (JwtException | DomainException rejected) {
            // A token we cannot make sense of means we do not know who is calling. That is
            // not a server fault, so it must not escape as one: DomainException is caught
            // because a subject that is not a UUID is rejected by the domain's own guard.
            return Optional.empty();
        }
    }

    private void rememberForThisRequest(SignedInUser user) {
        var authorities = List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.role()));
        // Credentials are null: the token was already verified, and keeping a copy of it in
        // the security context would serve nothing but a leak.
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
    }
}
