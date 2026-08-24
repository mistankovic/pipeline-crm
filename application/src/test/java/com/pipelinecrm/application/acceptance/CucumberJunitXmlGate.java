package com.pipelinecrm.application.acceptance;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public final class CucumberJunitXmlGate {

    static final int MINIMUM_SCENARIOS = 48;

    private CucumberJunitXmlGate() {}

    public static void main(String[] args) {
        Path report = Path.of(args.length == 0 ? "target/cucumber-junit.xml" : args[0]);
        String failure = check(report);
        if (failure != null) {
            System.err.println(failure);
            System.exit(1);
        }
    }

    static String check(Path report) {
        if (!Files.isRegularFile(report)) {
            return "Missing cucumber JUnit XML: " + report.toAbsolutePath()
                    + " (empty features/ or a Surefire that never ran Cucumber)";
        }
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            document = factory.newDocumentBuilder().parse(report.toFile());
        } catch (Exception ex) {
            return "Unreadable cucumber JUnit XML: " + report + " (" + ex.getMessage() + ")";
        }
        int tests = document.getElementsByTagName("testcase").getLength();
        int failures = document.getElementsByTagName("failure").getLength();
        int errors = document.getElementsByTagName("error").getLength();
        if (tests < MINIMUM_SCENARIOS) {
            return "Cucumber JUnit XML has " + tests + " testcases; need >= " + MINIMUM_SCENARIOS;
        }
        if (failures > 0 || errors > 0) {
            return "Cucumber JUnit XML has failures=" + failures + " errors=" + errors;
        }
        return null;
    }
}
