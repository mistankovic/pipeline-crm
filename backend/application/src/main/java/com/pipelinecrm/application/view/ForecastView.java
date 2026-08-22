package com.pipelinecrm.application.view;

import java.util.List;

/** A whole forecast, one line per group and currency. */
public record ForecastView(String dimension, List<ForecastLineView> lines) {
}
