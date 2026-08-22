package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ForecastView;

/** Weighted value of the open pipeline, grouped by owner or by stage. */
public interface ProduceForecast {

    ForecastView handle(String dimension);
}
