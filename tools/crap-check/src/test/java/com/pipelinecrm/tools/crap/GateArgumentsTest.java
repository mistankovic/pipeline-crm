package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GateArgumentsTest {

    @Test
    void parsesRequiredAndOptionalFlags() {
        GateArguments arguments = GateArguments.parse(new String[] {
            "--report",
            "target/jacoco.xml",
            "--threshold",
            "4.5",
            "--classes-dir",
            "target/classes",
            "--package",
            "com.pipelinecrm.domain",
            "--package",
            "com.pipelinecrm.application"
        });
        assertThat(arguments.report()).isEqualTo(Path.of("target/jacoco.xml"));
        assertThat(arguments.classesDir()).isEqualTo(Path.of("target/classes"));
        assertThat(arguments.threshold()).isEqualTo(4.5);
        assertThat(arguments.packages())
                .containsExactly("com.pipelinecrm.domain", "com.pipelinecrm.application");
    }

    @Test
    void defaultThresholdIsSix() {
        GateArguments arguments = GateArguments.parse(
                new String[] {"--report", "r.xml", "--package", "com.pipelinecrm.domain"});
        assertThat(arguments.threshold()).isEqualTo(6.0);
        assertThat(arguments.classesDir()).isNull();
    }

    @Test
    void rejectsUnknownFlag() {
        assertThatThrownBy(() -> GateArguments.parse(new String[] {"--nope"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void rejectsMissingValuesAndRequiredFlags() {
        assertThatThrownBy(() -> GateArguments.parse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GateArguments.parse(new String[] {"--report"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GateArguments.parse(new String[] {"--package", "p"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--report");
        assertThat(GateArguments.parse(new String[] {"--report", "r.xml"}).packages()).isEmpty();
        assertThatThrownBy(() -> GateArguments.parse(new String[] {"--report", "r.xml", "--threshold"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GateArguments.parse(new String[] {"--report", "r.xml", "--classes-dir"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GateArguments.parse(new String[] {"--report", "r.xml", "--package"}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonNumericOrNegativeThreshold() {
        assertThatThrownBy(() -> GateArguments.parse(
                        new String[] {"--report", "r.xml", "--package", "p", "--threshold", "x"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GateArguments.parse(
                        new String[] {"--report", "r.xml", "--package", "p", "--threshold", "-1"}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
