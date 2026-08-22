package com.pipelinecrm.adapter.web.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSettingsTest {

    @Test
    void refuses_to_start_without_a_secret() {
        assertThatThrownBy(() -> new JwtSettings(null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void refuses_a_secret_that_is_too_short_to_sign_with() {
        assertThatThrownBy(() -> new JwtSettings("too-short", null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void defaults_the_validity_to_eight_hours() {
        assertThat(new JwtSettings(JwtTestSupport.SECRET, null, null).validity()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void defaults_the_issuer() {
        assertThat(new JwtSettings(JwtTestSupport.SECRET, null, null).issuer()).isEqualTo("pipelinecrm");
    }

    @Test
    void keeps_a_configured_validity() {
        assertThat(new JwtSettings(JwtTestSupport.SECRET, Duration.ofMinutes(5), "x").validity())
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void keeps_a_configured_issuer() {
        assertThat(new JwtSettings(JwtTestSupport.SECRET, null, "acme").issuer()).isEqualTo("acme");
    }
}
