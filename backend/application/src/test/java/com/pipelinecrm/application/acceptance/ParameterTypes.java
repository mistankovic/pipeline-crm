package com.pipelinecrm.application.acceptance;

import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.forecast.ForecastDimension;
import io.cucumber.java.ParameterType;

import java.math.BigDecimal;

/** How the words in a feature file become values. */
public class ParameterTypes {

    @ParameterType("(-?\\d+(?:\\.\\d+)?) ([A-Za-z]{3})")
    public Amount money(String value, String currency) {
        return Amount.of(new BigDecimal(value), currency);
    }

    @ParameterType("(-?\\d+(?:\\.\\d+)?) ([A-Za-z]{3}) at (\\d+)% probability")
    public Amount amount(String value, String currency, String probability) {
        return new Amount(new BigDecimal(value), currency, Integer.parseInt(probability));
    }

    @ParameterType("LEAD|QUALIFIED|PROPOSAL|NEGOTIATION|CLOSED_WON|CLOSED_LOST")
    public DealStage stage(String name) {
        return DealStage.valueOf(name);
    }

    @ParameterType("OWNER|STAGE")
    public ForecastDimension dimension(String name) {
        return ForecastDimension.valueOf(name);
    }
}
