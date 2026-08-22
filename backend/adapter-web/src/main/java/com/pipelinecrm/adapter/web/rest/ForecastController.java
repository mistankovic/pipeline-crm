package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.application.port.in.ProduceForecast;
import com.pipelinecrm.application.view.ForecastView;
import com.pipelinecrm.domain.forecast.ForecastDimension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The weighted pipeline, sliced by owner or by stage. */
@RestController
public class ForecastController {

    private final ProduceForecast forecast;

    public ForecastController(ProduceForecast forecast) {
        this.forecast = forecast;
    }

    @GetMapping("/api/forecast")
    public ForecastView by(@RequestParam(name = "by", defaultValue = "OWNER") String dimension) {
        return forecast.handle(RequestedEnum.of(ForecastDimension.class, dimension, "by"));
    }
}
