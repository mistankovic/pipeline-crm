package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.domain.identity.UserId;
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

    public JwtAuthenticationFilter(JwtSettings settings) {
        this.settings = settings;
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
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new SignedInUser(
                    UserId.fromString(claims.getSubject()), claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException rejected) {
            return Optional.empty();
        }
    }

    private void rememberForThisRequest(SignedInUser user) {
        var authorities = List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.role()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, token(), authorities));
    }

    private String token() {
        return "n/a";
    }
}
