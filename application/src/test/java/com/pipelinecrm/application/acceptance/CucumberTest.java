package com.pipelinecrm.application.acceptance;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite(failIfNoTests = true)
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.pipelinecrm.application.acceptance")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty,junit:target/cucumber-junit.xml")
public class CucumberTest {}
