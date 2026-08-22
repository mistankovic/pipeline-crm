package com.pipelinecrm.application.view;

/** One row of a forecast: a group, and what that group's open pipeline is worth. */
public record ForecastLineView(String group, String label, MoneyView weightedValue) {
}
