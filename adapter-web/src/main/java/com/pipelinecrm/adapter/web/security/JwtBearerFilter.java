package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.domain.identity.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtBearerFilter extends OncePerRequestFilter {

    private final JwtSettings settings;

    public JwtBearerFilter(JwtSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring("Bearer ".length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UserId userId = UserId.parse(claims.getSubject());
            String role = String.valueOf(claims.get("role"));
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(settings.secret().getBytes(StandardCharsets.UTF_8));
    }
}
