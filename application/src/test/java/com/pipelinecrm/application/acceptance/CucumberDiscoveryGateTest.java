package com.pipelinecrm.application.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathResource;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

class CucumberDiscoveryGateTest {

    static final int MINIMUM_SCENARIOS = 48;

    @Test
    void featuresDirectoryYieldsAtLeastTheRequiredScenarios() {
        assertThat(discoveredTests("features"))
                .as("Gherkin scenarios under classpath:features must be discovered")
                .isGreaterThanOrEqualTo(MINIMUM_SCENARIOS);
    }

    @Test
    void missingFeaturesResourceDiscoversNothing() {
        assertThat(discoveredTests("features-that-do-not-exist"))
                .as("a vanished features directory must produce zero cucumber tests")
                .isZero();
    }

    private static long discoveredTests(String classpathResource) {
        LauncherDiscoveryRequest request = request()
                .selectors(selectClasspathResource(classpathResource))
                .filters(includeEngines("cucumber"))
                .configurationParameter(
                        Constants.GLUE_PROPERTY_NAME, "com.pipelinecrm.application.acceptance")
                .build();
        TestPlan plan = LauncherFactory.create().discover(request);
        return plan.countTestIdentifiers(TestIdentifier::isTest);
    }
}
