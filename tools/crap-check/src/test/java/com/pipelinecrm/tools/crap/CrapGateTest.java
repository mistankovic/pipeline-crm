package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrapGateTest {

    @Test
    void usageErrorWhenArgumentsInvalid() {
        Streams streams = new Streams();
        int code = streams.gate().run(new String[] {"--nope"});
        assertThat(code).isEqualTo(CrapGate.EXIT_USAGE);
        assertThat(streams.err()).contains("unknown");
    }

    @Test
    void skipsWhenReportAndProductionClassesAreMissing(@TempDir Path temp) {
        Streams streams = new Streams();
        int code = streams.gate()
                .run(new String[] {
                    "--report", temp.resolve("missing.xml").toString(),
                    "--classes-dir", temp.resolve("classes").toString(),
                    "--package", "com.pipelinecrm.domain"
                });
        assertThat(code).isEqualTo(CrapGate.EXIT_OK);
        assertThat(streams.out()).contains("skipped");
    }

    @Test
    void failsWhenReportMissingButClassesExist(@TempDir Path temp) throws IOException {
        Path classes = temp.resolve("classes/com/pipelinecrm/domain");
        Files.createDirectories(classes);
        Files.writeString(classes.resolve("Deal.class"), "x");
        Streams streams = new Streams();
        int code = streams.gate()
                .run(new String[] {
                    "--report", temp.resolve("missing.xml").toString(),
                    "--classes-dir", temp.resolve("classes").toString(),
                    "--package", "com.pipelinecrm.domain"
                });
        assertThat(code).isEqualTo(CrapGate.EXIT_VIOLATION);
        assertThat(streams.err()).contains("production classes exist");
    }

    @Test
    void passesSampleReportUnderThreshold() {
        Streams streams = new Streams();
        Path report = JacocoReportParserTest.resource("jacoco-sample.xml");
        int code = streams.gate()
                .run(new String[] {
                    "--report", report.toString(),
                    "--package", "com.pipelinecrm.domain"
                });
        assertThat(code).isEqualTo(CrapGate.EXIT_OK);
        assertThat(streams.out()).contains("checked: 1 methods").contains("OK");
    }

    @Test
    void withoutPackageFilterChecksEveryExecutableMethodInTheReport() {
        Streams streams = new Streams();
        Path report = JacocoReportParserTest.resource("jacoco-sample.xml");
        int code = streams.gate().run(new String[] {"--report", report.toString()});
        assertThat(code).isEqualTo(CrapGate.EXIT_VIOLATION);
        assertThat(streams.out()).contains("noise");
    }

    @Test
    void failsWhenMethodExceedsThreshold() {
        Streams streams = new Streams();
        Path report = JacocoReportParserTest.resource("jacoco-violation.xml");
        int code = streams.gate()
                .run(new String[] {
                    "--report", report.toString(),
                    "--package", "com.pipelinecrm.domain"
                });
        assertThat(code).isEqualTo(CrapGate.EXIT_VIOLATION);
        assertThat(streams.out()).contains("VIOLATION").contains("advance");
    }

    @Test
    void mapsParserFailuresToUsageExit() {
        Streams streams = new Streams();
        Path report = JacocoReportParserTest.resource("jacoco-broken.xml");
        int code = streams.gate()
                .run(new String[] {
                    "--report", report.toString(),
                    "--package", "com.pipelinecrm.domain"
                });
        assertThat(code).isEqualTo(CrapGate.EXIT_USAGE);
        assertThat(streams.err()).contains("failed to parse");
    }

    @Test
    void publicConstructorAndExecuteHandleUsageErrors() {
        int code = new CrapGate().run(new String[] {"--nope"});
        assertThat(code).isEqualTo(CrapGate.EXIT_USAGE);
        assertThat(CrapGate.execute(new String[] {"--nope"})).isEqualTo(CrapGate.EXIT_USAGE);
    }

    @Test
    void exceptionConstructorsPreserveCause() {
        CrapCheckException plain = new CrapCheckException("plain");
        CrapCheckException wrapped = new CrapCheckException("wrapped", new IllegalStateException("boom"));
        assertThat(plain.getMessage()).isEqualTo("plain");
        assertThat(wrapped).hasCauseInstanceOf(IllegalStateException.class);
    }

    private static final class Streams {
        private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

        private CrapGate gate() {
            PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
            PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
            return new CrapGate(out, err, new JacocoReportParser(), new CrapEvaluator(), new ProductionClassDetector());
        }

        private String out() {
            return outBytes.toString(StandardCharsets.UTF_8);
        }

        private String err() {
            return errBytes.toString(StandardCharsets.UTF_8);
        }
    }
}
