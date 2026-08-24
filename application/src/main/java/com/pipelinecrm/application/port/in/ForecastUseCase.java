package com.pipelinecrm.application.port.in;

import com.pipelinecrm.domain.forecast.ForecastBucket;
import java.util.Currency;
import java.util.List;

public interface ForecastUseCase {

    List<ForecastBucket> byOwner(Currency currency);

    List<ForecastBucket> byStage(Currency currency);
}
