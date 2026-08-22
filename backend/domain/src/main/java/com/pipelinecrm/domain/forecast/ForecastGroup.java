package com.pipelinecrm.domain.forecast;

/**
 * What a forecast line is grouped by. Sealed, so that a reader of a forecast can ask what
 * kind of group it holds and get back a typed answer rather than parsing a string.
 */
public sealed interface ForecastGroup permits OwnerGroup, StageGroup {
}
