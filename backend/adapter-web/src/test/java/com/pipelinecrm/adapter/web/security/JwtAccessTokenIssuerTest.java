package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.application.port.out.AccessTokenIssuer;
import com.pipelinecrm.domain.identity.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAccessTokenIssuerTest {

    private final JwtAccessTokenIssuer issuer =
            new JwtAccessTokenIssuer(JwtTestSupport.settings(), JwtTestSupport.frozenClock());

    private Claims claimsIn(String token) {
        return Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        JwtTestSupport.SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> java.util.Date.from(JwtTestSupport.NOW))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    void names_the_user_as_the_subject() {
        UserId id = JwtTestSupport.someUserId();

        AccessTokenIssuer.IssuedToken token = issuer.issueFor(JwtTestSupport.someUser(id));

        assertThat(claimsIn(token.value()).getSubject()).isEqualTo(id.value().toString());
    }

    @Test
    void carries_the_users_role() {
        AccessTokenIssuer.IssuedToken token = issuer.issueFor(JwtTestSupport.someUser(JwtTestSupport.someUserId()));

        assertThat(claimsIn(token.value()).get("role", String.class)).isEqualTo("SALES");
    }

    @Test
    void stamps_the_configured_issuer() {
        AccessTokenIssuer.IssuedToken token = issuer.issueFor(JwtTestSupport.someUser(JwtTestSupport.someUserId()));

        assertThat(claimsIn(token.value()).getIssuer()).isEqualTo(JwtTestSupport.ISSUER);
    }

    @Test
    void expires_after_the_configured_validity() {
        AccessTokenIssuer.IssuedToken token = issuer.issueFor(JwtTestSupport.someUser(JwtTestSupport.someUserId()));

        assertThat(token.expiresAt()).isEqualTo(JwtTestSupport.NOW.plus(Duration.ofHours(8)));
    }

    @Test
    void reports_the_same_expiry_it_wrote_into_the_token() {
        AccessTokenIssuer.IssuedToken token = issuer.issueFor(JwtTestSupport.someUser(JwtTestSupport.someUserId()));

        assertThat(claimsIn(token.value()).getExpiration().toInstant()).isEqualTo(token.expiresAt());
    }

    @Test
    void never_puts_the_email_address_in_the_token() {
        AccessTokenIssuer.IssuedToken token = issuer.issueFor(JwtTestSupport.someUser(JwtTestSupport.someUserId()));

        assertThat(token.value()).doesNotContain("sam@example.com");
    }
}
