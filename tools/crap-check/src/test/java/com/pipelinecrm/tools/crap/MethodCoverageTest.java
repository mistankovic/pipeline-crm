package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MethodCoverageTest {

    @Test
    void executableWhenComplexityAndLinesExist() {
        MethodCoverage method = sample("advance", 3, 4, 1);
        assertThat(method.isExecutable()).isTrue();
        assertThat(method.lineCoverageRatio()).isEqualTo(0.8);
        assertThat(method.identity()).isEqualTo("com.pipelinecrm.domain.Deal#advance()V");
    }

    @Test
    void notExecutableWithoutLinesOrComplexity() {
        assertThat(sample("empty", 0, 2, 0).isExecutable()).isFalse();
        assertThat(sample("abstract", 1, 0, 0).isExecutable()).isFalse();
        assertThat(sample("abstract", 1, 0, 0).lineCoverageRatio()).isEqualTo(1.0);
    }

    @Test
    void simpleClassNameUsesWholeNameWhenNoSlash() {
        MethodCoverage method = new MethodCoverage("p", "Deal", "m", "()V", 1, 1, 0);
        assertThat(method.identity()).isEqualTo("p.Deal#m()V");
    }

    @Test
    void rejectsNullIdentity() {
        assertThatThrownBy(() -> new MethodCoverage(null, "C", "m", "()V", 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MethodCoverage("p", null, "m", "()V", 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MethodCoverage("p", "C", null, "()V", 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MethodCoverage("p", "C", "m", null, 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeCounters() {
        assertThatThrownBy(() -> new MethodCoverage("p", "C", "m", "()V", -1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MethodCoverage("p", "C", "m", "()V", 0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MethodCoverage("p", "C", "m", "()V", 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static MethodCoverage sample(String name, int complexity, int covered, int missed) {
        return new MethodCoverage(
                "com.pipelinecrm.domain", "com/pipelinecrm/domain/Deal", name, "()V", complexity, covered, missed);
    }
}
