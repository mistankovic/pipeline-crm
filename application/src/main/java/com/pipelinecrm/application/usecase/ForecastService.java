package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ForecastUseCase;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.domain.forecast.Forecast;
import com.pipelinecrm.domain.forecast.ForecastBucket;
import java.util.Currency;
import java.util.List;

public final class ForecastService implements ForecastUseCase {

    private final DealRepository deals;

    public ForecastService(DealRepository deals) {
        this.deals = deals;
    }

    @Override
    public List<ForecastBucket> byOwner(Currency currency) {
        return Forecast.byOwner(deals.findAll(), currency);
    }

    @Override
    public List<ForecastBucket> byStage(Currency currency) {
        return Forecast.byStage(deals.findAll(), currency);
    }
}
