package com.pipelinecrm.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProbabilityTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 50, 99, 100})
    void accepts_every_whole_percentage_from_zero_to_one_hundred(int percentage) {
        assertThat(Probability.of(percentage).percentage()).isEqualTo(percentage);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejects_anything_outside_that_range(int percentage) {
        assertThatThrownBy(() -> Probability.of(percentage))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void certainty_is_one_hundred_percent() {
        assertThat(Probability.certain()).isEqualTo(Probability.of(100));
    }

    @Test
    void impossibility_is_zero_percent() {
        assertThat(Probability.impossible()).isEqualTo(Probability.of(0));
    }

    @Test
    void expresses_itself_as_a_fraction_for_arithmetic() {
        assertThat(Probability.of(25).asFraction()).isEqualByComparingTo(new BigDecimal("0.25"));
    }
}
