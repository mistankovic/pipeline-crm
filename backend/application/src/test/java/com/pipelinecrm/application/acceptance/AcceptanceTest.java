package com.pipelinecrm.application.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runs every feature against the **use-case layer**: no HTTP, no database, no browser. What
 * these scenarios prove is that the business rules hold; that they hold over a wire and a
 * table is a different question, answered by different tests.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.pipelinecrm.application.acceptance")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "summary, html:target/cucumber.html")
class AcceptanceTest {
}
