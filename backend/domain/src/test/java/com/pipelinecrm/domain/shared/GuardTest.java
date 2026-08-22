package com.pipelinecrm.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardTest {

    @Test
    void returns_a_value_that_is_present() {
        Object value = new Object();

        assertThat(Guard.present(value, "thing")).isSameAs(value);
    }

    @Test
    void names_the_missing_subject_so_the_message_is_useful() {
        assertThatThrownBy(() -> Guard.present(null, "owner of a deal"))
                .isInstanceOf(InvariantViolation.class)
                .hasMessage("owner of a deal is required");
    }

    @Test
    void strips_a_filled_string() {
        assertThat(Guard.filled("  Acme  ", "name")).isEqualTo("Acme");
    }

    @Test
    void rejects_a_null_string() {
        assertThatThrownBy(() -> Guard.filled(null, "name"))
                .isInstanceOf(InvariantViolation.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void rejects_a_whitespace_only_string() {
        assertThatThrownBy(() -> Guard.filled("\t\n ", "name")).isInstanceOf(InvariantViolation.class);
    }
}
