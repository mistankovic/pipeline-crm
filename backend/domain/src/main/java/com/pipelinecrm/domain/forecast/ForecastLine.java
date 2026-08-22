package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.shared.Guard;
import com.pipelinecrm.domain.shared.Money;

/** One row of a forecast: a group of open deals, and what that group is worth once weighted. */
public record ForecastLine(ForecastGroup group, Money weightedValue) {

    public ForecastLine {
        Guard.present(group, "forecast group");
        Guard.present(weightedValue, "weighted value");
    }
}
