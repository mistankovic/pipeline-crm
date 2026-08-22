package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.Money;

/** One row of a forecast: what a group of open deals is worth once weighted by probability. */
public record ForecastLine(String group, Money weightedValue) {

    public ForecastLine {
        group = Guard.filled(group, "forecast group");
        Guard.present(weightedValue, "weighted value");
    }
}
