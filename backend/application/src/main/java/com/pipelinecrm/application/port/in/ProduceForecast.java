package com.pipelinecrm.application.port.in;

import com.pipelinecrm.application.view.ForecastView;
import com.pipelinecrm.domain.forecast.ForecastDimension;

/**
 * Weighted value of the open pipeline, grouped by owner or by stage. The dimension is the
 * domain's own enum: turning the caller's text into it is the web adapter's job, one layer
 * out, and a use case should not be handed a string it has to validate.
 */
public interface ProduceForecast {

    ForecastView handle(ForecastDimension dimension);
}
