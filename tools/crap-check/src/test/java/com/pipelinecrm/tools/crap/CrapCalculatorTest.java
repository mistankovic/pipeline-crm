package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class CrapCalculatorTest {

    @Test
    void fullCoverageMakesCrapEqualComplexity() {
        assertThat(CrapCalculator.crap(1, 1.0)).isCloseTo(1.0, within(1e-9));
        assertThat(CrapCalculator.crap(6, 1.0)).isCloseTo(6.0, within(1e-9));
    }

    @Test
    void zeroCoverageSquaresComplexityThenAddsIt() {
        assertThat(CrapCalculator.crap(2, 0.0)).isCloseTo(6.0, within(1e-9));
        assertThat(CrapCalculator.crap(3, 0.0)).isCloseTo(12.0, within(1e-9));
    }

    @Test
    void halfCoverageOnComplexityFiveExceedsThresholdSix() {
        double crap = CrapCalculator.crap(5, 0.5);
        assertThat(crap).isCloseTo(8.125, within(1e-9));
        assertThat(CrapCalculator.exceeds(crap, 6.0)).isTrue();
    }

    @Test
    void complexitySixAtFullCoverageDoesNotExceedSix() {
        assertThat(CrapCalculator.exceeds(CrapCalculator.crap(6, 1.0), 6.0)).isFalse();
    }

    @Test
    void rejectsNegativeComplexity() {
        assertThatThrownBy(() -> CrapCalculator.crap(-1, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("complexity");
    }

    @Test
    void rejectsCoverageOutsideUnitInterval() {
        assertThatThrownBy(() -> CrapCalculator.crap(1, -0.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrapCalculator.crap(1, 1.01)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeThreshold() {
        assertThatThrownBy(() -> CrapCalculator.exceeds(1.0, -1.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
