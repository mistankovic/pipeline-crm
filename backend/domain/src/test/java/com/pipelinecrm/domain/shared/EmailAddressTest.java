package com.pipelinecrm.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void stores_the_address_in_lower_case_so_that_two_spellings_are_one_address() {
        assertThat(EmailAddress.of("Sam.Sales@Example.COM"))
                .isEqualTo(EmailAddress.of("sam.sales@example.com"));
    }

    @Test
    void strips_surrounding_whitespace() {
        assertThat(EmailAddress.of("  sam@example.com  ").value()).isEqualTo("sam@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sam", "sam@", "@example.com", "sam@example", "sam @example.com",
            "sam@exam ple.com", "sam@@example.com"})
    void rejects_anything_that_is_not_shaped_like_an_address(String candidate) {
        assertThatThrownBy(() -> EmailAddress.of(candidate))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("not an email address");
    }

    @Test
    void rejects_a_blank_address() {
        assertThatThrownBy(() -> EmailAddress.of("   "))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejects_a_missing_address() {
        assertThatThrownBy(() -> EmailAddress.of(null)).isInstanceOf(InvariantViolation.class);
    }
}
