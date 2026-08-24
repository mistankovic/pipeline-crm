package com.pipelinecrm.application.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CucumberJunitXmlGateTest {

    @TempDir
    Path tempDir;

    @Test
    void missingReportFails() {
        Path missing = tempDir.resolve("absent.xml");
        assertThat(CucumberJunitXmlGate.check(missing)).contains("Missing cucumber JUnit XML");
    }

    @Test
    void zeroTestcasesFails() throws Exception {
        Path report = tempDir.resolve("empty.xml");
        Files.writeString(report, "<testsuite name=\"Cucumber\" tests=\"0\" failures=\"0\" errors=\"0\"/>");
        assertThat(CucumberJunitXmlGate.check(report)).contains("0 testcases");
    }

    @Test
    void enoughPassingTestcasesSucceeds() throws Exception {
        Path report = tempDir.resolve("ok.xml");
        Files.writeString(report, suiteXml(CucumberJunitXmlGate.MINIMUM_SCENARIOS, 0));
        assertThat(CucumberJunitXmlGate.check(report)).isNull();
    }

    @Test
    void failuresFailTheGate() throws Exception {
        Path report = tempDir.resolve("failed.xml");
        Files.writeString(report, suiteXml(CucumberJunitXmlGate.MINIMUM_SCENARIOS, 1));
        assertThat(CucumberJunitXmlGate.check(report)).contains("failures=1");
    }

    private static String suiteXml(int tests, int failures) {
        StringBuilder xml = new StringBuilder();
        xml.append("<testsuite name=\"Cucumber\" tests=\"")
                .append(tests)
                .append("\" failures=\"")
                .append(failures)
                .append("\" errors=\"0\">");
        for (int i = 0; i < tests; i++) {
            xml.append("<testcase name=\"scenario-").append(i).append("\" classname=\"Feature\">");
            if (i < failures) {
                xml.append("<failure message=\"boom\"/>");
            }
            xml.append("</testcase>");
        }
        xml.append("</testsuite>");
        return xml.toString();
    }
}
