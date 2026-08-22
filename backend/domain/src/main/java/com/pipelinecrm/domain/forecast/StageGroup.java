package com.pipelinecrm.domain.forecast;

import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.shared.Guard;

/** A forecast line for one pipeline stage. */
public record StageGroup(DealStage stage) implements ForecastGroup {

    public StageGroup {
        Guard.present(stage, "stage a forecast is grouped by");
    }
}
