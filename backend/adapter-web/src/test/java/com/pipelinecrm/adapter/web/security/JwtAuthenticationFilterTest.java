package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.domain.identity.UserId;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The filter decides who is calling. Everything it can be handed that is not a valid token
 * must leave the security context empty — and must still let the request through, so that
 * the security configuration, not the filter, decides what an anonymous request may reach.
 */
class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(JwtTestSupport.settings(), JwtTestSupport.frozenClock());
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication authenticationAfterFiltering(String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader("Authorization", headerValue);
        }

        filter.doFilter(request, response, chain);

        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String validTokenFor(UserId id) {
        return JwtTestSupport.tokenSignedWith(JwtTestSupport.SECRET, JwtTestSupport.ISSUER,
                id.value().toString(), JwtTestSupport.NOW.plus(Duration.ofHours(1)));
    }

    @Test
    void recognises_the_user_named_by_a_valid_token() throws Exception {
        UserId id = JwtTestSupport.someUserId();

        Authentication authentication = authenticationAfterFiltering("Bearer " + validTokenFor(id));

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new SignedInUser(id, "SALES"));
    }

    @Test
    void grants_the_role_the_token_claims() throws Exception {
        Authentication authentication =
                authenticationAfterFiltering("Bearer " + validTokenFor(JwtTestSupport.someUserId()));

        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SALES");
    }

    @Test
    void keeps_no_copy_of_the_token_as_credentials() throws Exception {
        Authentication authentication =
                authenticationAfterFiltering("Bearer " + validTokenFor(JwtTestSupport.someUserId()));

        assertThat(authentication.getCredentials()).isNull();
    }

    @Test
    void recognises_nobody_when_there_is_no_header() throws Exception {
        assertThat(authenticationAfterFiltering(null)).isNull();
    }

    @Test
    void recognises_nobody_when_the_header_is_not_a_bearer_header() throws Exception {
        assertThat(authenticationAfterFiltering("Basic c2FtOnNlY3JldA==")).isNull();
    }

    @Test
    void recognises_nobody_when_the_token_is_gibberish() throws Exception {
        assertThat(authenticationAfterFiltering("Bearer not.a.token")).isNull();
    }

    @Test
    void recognises_nobody_when_the_token_was_signed_with_another_secret() throws Exception {
        String forged = JwtTestSupport.tokenSignedWith(JwtTestSupport.OTHER_SECRET, JwtTestSupport.ISSUER,
                JwtTestSupport.someUserId().value().toString(), JwtTestSupport.NOW.plus(Duration.ofHours(1)));

        assertThat(authenticationAfterFiltering("Bearer " + forged)).isNull();
    }

    @Test
    void recognises_nobody_when_the_token_came_from_another_issuer() throws Exception {
        String foreign = JwtTestSupport.tokenSignedWith(JwtTestSupport.SECRET, "somebody-else",
                JwtTestSupport.someUserId().value().toString(), JwtTestSupport.NOW.plus(Duration.ofHours(1)));

        assertThat(authenticationAfterFiltering("Bearer " + foreign)).isNull();
    }

    @Test
    void recognises_nobody_when_the_token_has_expired() throws Exception {
        String expired = JwtTestSupport.tokenSignedWith(JwtTestSupport.SECRET, JwtTestSupport.ISSUER,
                JwtTestSupport.someUserId().value().toString(), JwtTestSupport.NOW.minus(Duration.ofSeconds(1)));

        assertThat(authenticationAfterFiltering("Bearer " + expired)).isNull();
    }

    @Test
    void recognises_nobody_when_the_subject_is_not_an_identity() throws Exception {
        String nonsense = JwtTestSupport.tokenSignedWith(JwtTestSupport.SECRET, JwtTestSupport.ISSUER,
                "not-a-uuid", JwtTestSupport.NOW.plus(Duration.ofHours(1)));

        assertThat(authenticationAfterFiltering("Bearer " + nonsense)).isNull();
    }

    @Test
    void always_lets_the_request_continue_so_that_the_security_rules_decide() throws Exception {
        authenticationAfterFiltering("Bearer not.a.token");

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
