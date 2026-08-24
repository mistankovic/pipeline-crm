package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class JacocoReportParserTest {

    private final JacocoReportParser parser = new JacocoReportParser();

    @Test
    void parsesMethodsAndIgnoresExternalDtd() {
        List<MethodCoverage> methods = parser.parse(resource("jacoco-sample.xml"));
        assertThat(methods).hasSize(3);
        MethodCoverage advance = methods.getFirst();
        assertThat(advance.packageName()).isEqualTo("com.pipelinecrm.domain");
        assertThat(advance.methodName()).isEqualTo("advance");
        assertThat(advance.complexity()).isEqualTo(2);
        assertThat(advance.linesCovered()).isEqualTo(4);
        assertThat(advance.linesMissed()).isEqualTo(0);
        assertThat(advance.isExecutable()).isTrue();
        assertThat(methods.get(1).isExecutable()).isFalse();
    }

    @Test
    void treatsBlankCountersAsZero() {
        List<MethodCoverage> methods = parser.parse(resource("jacoco-violation.xml"));
        MethodCoverage blank = methods.get(1);
        assertThat(blank.complexity()).isZero();
        assertThat(blank.linesCovered()).isZero();
        assertThat(blank.linesMissed()).isZero();
    }

    @Test
    void rejectsBrokenXml() {
        assertThatThrownBy(() -> parser.parse(resource("jacoco-broken.xml")))
                .isInstanceOf(CrapCheckException.class)
                .hasMessageContaining("failed to parse");
    }

    @Test
    void rejectsNonNumericCounters() {
        assertThatThrownBy(() -> parser.parse(resource("jacoco-bad-counter.xml")))
                .isInstanceOf(CrapCheckException.class)
                .hasMessageContaining("invalid counter");
    }

    static Path resource(String name) {
        try {
            return Path.of(JacocoReportParserTest.class.getResource("/" + name).toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
