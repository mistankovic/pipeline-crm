package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RequireTest {

    @Test
    void returnsValueWhenValid() {
        assertThat(Require.notNull("x", "name")).isEqualTo("x");
        assertThat(Require.notNegative(0, "n")).isZero();
        assertThat(Require.notNegative(0.0, "n")).isZero();
    }

    @Test
    void rejectsNullAndNegative() {
        assertThatThrownBy(() -> Require.notNull(null, "name")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Require.notNegative(-1, "n")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Require.notNegative(-0.1, "n")).isInstanceOf(IllegalArgumentException.class);
    }
}
