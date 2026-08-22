package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.port.in.ProduceForecast;
import com.pipelinecrm.application.port.out.DealRepository;
import com.pipelinecrm.application.view.ForecastView;
import com.pipelinecrm.application.view.ForecastViews;
import com.pipelinecrm.domain.forecast.Forecast;
import com.pipelinecrm.domain.forecast.ForecastCalculator;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.forecast.ForecastDimension;

import java.util.List;

/** The weighted value of the open pipeline. The arithmetic belongs to the domain. */
public final class ProduceForecastInteractor implements ProduceForecast {

    private final DealRepository deals;
    private final Parties parties;
    private final ForecastCalculator calculator;

    public ProduceForecastInteractor(DealRepository deals, Parties parties, ForecastCalculator calculator) {
        this.deals = deals;
        this.parties = parties;
        this.calculator = calculator;
    }

    @Override
    public ForecastView handle(ForecastDimension dimension) {
        List<Deal> open = deals.findAll();
        Forecast forecast = calculator.forecast(open, dimension);
        return ForecastViews.of(dimension, forecast, parties.indexFor(open).users());
    }
}
